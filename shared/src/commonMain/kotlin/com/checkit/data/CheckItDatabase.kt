package com.checkit.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
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
    val color: String,
    val sortOrder: Int = 0,
    val lastUsedAtMillis: Long = 0L
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ObjectiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["objectiveId"],
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
        Index("objectiveId"),
        Index("keyResultId"),
        Index("status"),
        Index("priority"),
        Index("doDateEpochDays")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val objectiveId: Long,
    val keyResultId: Long? = null,
    val name: String,
    val description: String = "",
    val status: String,
    val priority: String,
    val type: String = "Task",
    val doDateEpochDays: Int? = null,
    val completedDateEpochDays: Int? = null,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
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
            childColumns = ["objectiveId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("objectiveId")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val objectiveId: Long,
    val title: String = "",
    val content: String,
    val status: String = "Open",
    val dateEpochDays: Int? = null,
    val startTimeMinutes: Int? = null,
    val createdAtMillis: Long,
    val editedAtMillis: Long,
    val sortOrder: Int,
    val trashedAtMillis: Long? = null
)

@Entity(
    tableName = "daily_plan_items",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("dateEpochDays"), Index("taskId"), Index("status")]
)
data class DailyPlanItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateEpochDays: Int,
    val taskId: Long? = null,
    val title: String,
    val note: String? = null,
    val source: String,
    val status: String,
    val sortOrder: Int,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val isHabit: Boolean = false,
    val addedAtMillis: Long,
    val completedAtMillis: Long? = null,
    /** Id of the source item this was copied from via carry-over, if any. */
    val carriedFromItemId: Long? = null,
    /** Timestamp (epoch millis) when this item was resolved by a review or carry-over. */
    val handledAtMillis: Long? = null
)

@Entity(
    tableName = "period_reviews",
    indices = [Index(value = ["periodType", "periodStartEpochDays"], unique = true)]
)
data class PeriodReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val periodType: String,
    val periodStartEpochDays: Int,
    val periodEndEpochDays: Int,
    val title: String? = null,
    val content: String = "",
    val highlightsJson: String? = null,
    val intentNext: String? = null,
    val source: String = com.checkit.domain.ReviewSource.Manual.name,
    val status: String = com.checkit.domain.ReviewStatus.Draft.name,
    val completedAtMillis: Long? = null,
    val generatedAtMillis: Long? = null,
    val editedAtMillis: Long? = null,
    val statsJson: String? = null
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
    tableName = "journal_entries",
    indices = [Index("dateEpochDays")]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateEpochDays: Int,
    val context: String? = null,
    val content: String,
    val moods: String = "",
    val createdTimeMinutes: Int
)

@Entity(
    tableName = "journal_entry_tags",
    primaryKeys = ["entryId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId"), Index("tagId")]
)
data class JournalEntryTagEntity(
    val entryId: Long,
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
        DailyPlanItemEntity::class,
        PeriodReviewEntity::class,
        TagEntity::class,
        TaskTagEntity::class,
        NoteTagEntity::class,
        DailyPlanItemTagEntity::class,
        JournalEntryEntity::class,
        JournalEntryTagEntity::class,
        TaskReminderEntity::class,
        TaskFilterEntity::class
    ],
    version = 1,
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
        .addCallback(object : RoomDatabase.Callback() {
            override suspend fun onCreate(connection: SQLiteConnection) {
                super.onCreate(connection)
                seedDefaultFilters(connection)
                seedInboxObjective(connection)
            }
        })
        .build()
}

private fun seedDefaultFilters(connection: SQLiteConnection) {
    DefaultTaskFilters.forEach { filter ->
        val dueDatePreset = filter.dueDatePreset?.let { "'$it'" } ?: "NULL"
        val status = filter.status?.let { "'$it'" } ?: "NULL"
        val priority = filter.priority?.let { "'$it'" } ?: "NULL"
        val includeTrashed = if (filter.includeTrashed) 1 else 0
        connection.execSQL(
            """
            INSERT INTO task_filters(name, icon, color, tagId, dueDatePreset, status, priority, includeTrashed, sortOrder)
            SELECT '${filter.name}', '${filter.icon}', '${filter.color}', NULL, $dueDatePreset, $status, $priority, $includeTrashed, ${filter.sortOrder}
            WHERE NOT EXISTS(SELECT 1 FROM task_filters WHERE name = '${filter.name}')
            """.trimIndent()
        )
    }
}

private fun seedInboxObjective(connection: SQLiteConnection) {
    connection.execSQL(
        """
        INSERT INTO objectives(title, goalId, startDateEpochDays, endDateEpochDays, color, icon, sortOrder, isArchived)
        SELECT 'Inbox', NULL, NULL, NULL, '#2563EB', 'Inbox', 0, 0
        WHERE NOT EXISTS(SELECT 1 FROM objectives)
        """.trimIndent()
    )
}

private data class TaskFilterSeed(
    val name: String,
    val icon: String,
    val color: String,
    val dueDatePreset: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val includeTrashed: Boolean = false,
    val sortOrder: Int
)

private val DefaultTaskFilters = listOf(
    TaskFilterSeed(name = "All", icon = "AllInclusive", color = "#475569", sortOrder = 0),
    TaskFilterSeed(
        name = "Today",
        icon = "Today",
        color = "#2563EB",
        dueDatePreset = "Today",
        sortOrder = 1
    ),
    TaskFilterSeed(
        name = "Upcoming",
        icon = "Schedule",
        color = "#0891B2",
        dueDatePreset = "Upcoming",
        sortOrder = 2
    ),
    TaskFilterSeed(
        name = "Overdue",
        icon = "Flag",
        color = "#EA580C",
        dueDatePreset = "Overdue",
        sortOrder = 3
    ),
    TaskFilterSeed(
        name = "No date",
        icon = "Schedule",
        color = "#7C3AED",
        dueDatePreset = "NoDate",
        sortOrder = 4
    ),
    TaskFilterSeed(
        name = "Completed",
        icon = "TaskAlt",
        color = "#059669",
        status = "Completed",
        sortOrder = 5
    ),
    TaskFilterSeed(
        name = "High priority",
        icon = "PriorityHigh",
        color = "#DC2626",
        priority = "High",
        sortOrder = 6
    ),
    TaskFilterSeed(name = "Trashed", icon = "Delete", color = "#6B7280", includeTrashed = true, sortOrder = 7)
)

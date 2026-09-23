package com.checkit.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.migration.Migration
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.sqlite.SQLiteConnection
import com.checkit.domain.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
@Entity(
    tableName = "lists",
    indices = [Index("dirty"), Index("updatedAtMillis")]
)
data class ListEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val icon: String,
    val color: String,
    val sortOrder: Int,
    val isArchived: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the list is deleted locally; purged after upload. */
    val deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true),
        Index("dirty"),
        Index("updatedAtMillis")
    ]
)
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String,
    val sortOrder: Int = 0,
    /** Device-local usage recency; never synced (would fight last-write-wins). */
    val lastUsedAtMillis: Long = 0L,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the tag is deleted locally; purged after upload. */
    val deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "tasks",
    indices = [
        Index("status"),
        Index("priority"),
        Index("doDateEpochDays"),
        Index("dirty"),
        Index("updatedAtMillis")
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val status: String,
    val priority: String,
    val type: String = TaskType.Task.name,
    val doDateEpochDays: Int? = null,
    val completedDateEpochDays: Int? = null,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val repeatRRule: String? = null,
    val label: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val trashedAtMillis: Long? = null,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the task is deleted locally; purged after upload. */
    val deleted: Boolean = false
)

/**
 * Sub-tasks are embedded in the parent task's sync document, so they carry
 * no sync columns of their own; the parent's dirty/deleted flags cover them.
 */
@Serializable
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
    @PrimaryKey
    val id: String,
    val taskId: String,
    val name: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int
)

@Serializable
@Entity(
    tableName = "notes",
    indices = [Index("dirty"), Index("editedAtMillis")]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String = "",
    val content: String,
    val status: String = "Open",
    val dateEpochDays: Int? = null,
    val startTimeMinutes: Int? = null,
    val createdAtMillis: Long,
    /** Last-write-wins clock for sync. */
    val editedAtMillis: Long,
    val label: String? = null,
    val trashedAtMillis: Long? = null,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the note is deleted locally; purged after upload. */
    val deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "daily_plan_items",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = NestedListItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["nestedListItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("dateEpochDays"),
        Index("taskId"),
        Index("status"),
        Index("nestedListItemId"),
        Index("dirty"),
        Index("updatedAtMillis")
    ]
)
data class DailyPlanItemEntity(
    @PrimaryKey
    val id: String,
    val dateEpochDays: Int,
    val taskId: String? = null,
    val nestedListItemId: String? = null,
    val title: String,
    val note: String? = null,
    val source: String,
    val status: String,
    val sortOrder: Int,
    val label: String? = null,
    val startTimeMinutes: Int? = null,
    val endTimeMinutes: Int? = null,
    val isHabit: Boolean = false,
    val addedAtMillis: Long,
    val completedAtMillis: Long? = null,
    /** Id of the source item this was copied from via carry-over, if any. */
    val carriedFromItemId: String? = null,
    /** Timestamp (epoch millis) when this item was resolved by a review or carry-over. */
    val handledAtMillis: Long? = null,
    /** Last-write-wins clock for sync. */
    val updatedAtMillis: Long,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the item is deleted locally; purged after upload. */
    val deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "period_goals",
    indices = [
        Index(value = ["periodType", "startEpochDays"], unique = true),
        Index("dirty"),
        Index("updatedAtMillis")
    ]
)
data class PeriodGoalEntity(
    @PrimaryKey
    val id: String,
    val periodType: String,
    val startEpochDays: Int,
    val endEpochDays: Int,
    val review: String = "",
    val goal: String? = null,
    /** Satisfaction for this period (e.g. 0..5). */
    val rating: Float = 0f,
    val completedAtMillis: Long? = null,
    val editedAtMillis: Long? = null,
    /** Last-write-wins clock for sync; maintained alongside [editedAtMillis]. */
    val updatedAtMillis: Long,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the goal is deleted locally; purged after upload. */
    val deleted: Boolean = false,
    /** Custom metrics stored inline as JSON; always loaded/saved with the goal. */
    val metricsJson: String = "[]"
)

/** Precomputed daily aggregates feeding the Reflect tab. One row per day. */
@Entity(tableName = "daily_reflect_stats")
data class DailyReflectStatsEntity(
    @PrimaryKey
    val dateEpochDays: Int,
    /** Actionable items (MyDayTask/MyDayReminder/ExistingTask) still planned. */
    val plannedItemCount: Int,
    /** Actionable items completed. */
    val doneItemCount: Int,
    /** Sum of scheduled minutes across all done items. */
    val doneMinutes: Int,
    val journalCount: Int,
    val computedAtMillis: Long
)

/**
 * Precomputed done count/minutes per day and tag. Tag name/color are joined
 * from [TagEntity] at query time so renames and recolors stay in sync.
 */
@Entity(
    tableName = "daily_tag_rollups",
    primaryKeys = ["dateEpochDays", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class DailyTagRollupEntity(
    val dateEpochDays: Int,
    val tagId: String,
    val doneCount: Int,
    val doneMinutes: Int
)

/**
 * Precomputed habit check-ins per day. [habitKey] is stable across carry-overs:
 * `task:<taskId>` when the habit is task-backed, otherwise derived from the title.
 */
@Entity(
    tableName = "habit_daily_rollups",
    primaryKeys = ["dateEpochDays", "habitKey"],
    indices = [Index("habitKey")]
)
data class HabitDailyRollupEntity(
    val dateEpochDays: Int,
    val habitKey: String,
    val title: String,
    val doneMinutes: Int
)

/** Slim projection of daily_tag_rollups joined with tag metadata. */
data class DailyTagRollupWithMeta(
    val dateEpochDays: Int,
    val tagId: String,
    val tagName: String,
    val tagColor: String?,
    val doneCount: Int,
    val doneMinutes: Int
)

/** Slim projection of a done daily-plan item for highlights (no tags/labels). */
data class DoneItemSummaryEntity(
    val id: String,
    val dateEpochDays: Int,
    val title: String,
    val note: String?,
    val source: String,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val completedAtMillis: Long?
)

/** Tag membership syncs embedded in the parent document; no sync columns here. */
@Serializable
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
    val taskId: String,
    val tagId: String
)

@Serializable
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
    val noteId: String,
    val tagId: String
)

/** Projection row for per-tag usage counts (tasks, notes, daily plan items, journal entries). */
data class TagUsageCountEntity(
    val tagId: String,
    val usageCount: Int
)

@Serializable
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
    val itemId: String,
    val tagId: String
)

@Serializable
@Entity(
    tableName = "journal_entries",
    indices = [Index("dateEpochDays"), Index("dirty"), Index("updatedAtMillis")]
)
data class JournalEntryEntity(
    @PrimaryKey
    val id: String,
    val dateEpochDays: Int,
    val label: String? = null,
    val content: String,
    val moods: String = "",
    val createdTimeMinutes: Int,
    val attachments: String = "",
    /** Last-write-wins clock for sync. */
    val updatedAtMillis: Long,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the entry is deleted locally; purged after upload. */
    val deleted: Boolean = false
)

@Serializable
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
    val entryId: String,
    val tagId: String
)

@Serializable
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
    @PrimaryKey
    val id: String,
    val taskId: String,
    val remindAtMillis: Long,
    val label: String = ""
)

/**
 * Local-only UI configuration (seeded defaults); never synced.
 */
@Serializable
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
    @PrimaryKey
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val tagId: String? = null,
    val dueDatePreset: String? = null,
    val status: String? = null,
    val priority: String? = null,
    val includeTrashed: Boolean = false,
    val sortOrder: Int
)

@Serializable
@Entity(
    tableName = "list_sections",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId"), Index("dirty"), Index("updatedAtMillis")]
)
data class ListSectionEntity(
    @PrimaryKey
    val id: String,
    val listId: String,
    val title: String,
    val color: String,
    val sortOrder: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the section is deleted locally; purged after upload. */
    val deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "task_list",
    primaryKeys = ["taskId", "listId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ListSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("taskId"), Index("listId"), Index("sectionId")]
)
data class TaskListEntity(
    val taskId: String,
    val listId: String,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val sectionId: String? = null
)

@Serializable
@Entity(
    tableName = "note_list",
    primaryKeys = ["noteId", "listId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ListSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("noteId"), Index("listId"), Index("sectionId")]
)
data class NoteListEntity(
    val noteId: String,
    val listId: String,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val sectionId: String? = null
)

@Serializable
@Entity(
    tableName = "nested_documents",
    indices = [Index("dirty"), Index("updatedAtMillis")]
)
data class NestedDocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the document is deleted locally; purged after upload. */
    val deleted: Boolean = false
)

@Serializable
@Entity(
    tableName = "nested_list_items",
    foreignKeys = [
        ForeignKey(
            entity = NestedDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NestedListItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId"), Index("parentId"), Index("startDateEpochDays"), Index("endDateEpochDays"), Index("priority"), Index("dirty"), Index("updatedAtMillis")]
)
data class NestedListItemEntity(
    @PrimaryKey
    val id: String,
    val documentId: String,
    val parentId: String? = null,
    val position: Int,
    val text: String,
    val note: String? = null,
    val checkboxEnabled: Boolean = false,
    val checked: Boolean = false,
    val collapsed: Boolean = false,
    val textStyle: String = "Body",
    val textColor: String = "Default",
    val backgroundColor: String = "Default",
    val startDateEpochDays: Int? = null,
    val endDateEpochDays: Int? = null,
    val priority: String = "None",
    val actualMinutes: Int = 0,
    val metricRollupPolicy: String = "IncludeChildren",
    val showTrackedMinutes: Boolean = false,
    /** Manual progress 0..100; null means progress UI is hidden. */
    val progressPercent: Int? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the item is deleted locally; purged after upload. */
    val deleted: Boolean = false,
    /** Custom metrics stored inline as JSON; always loaded/saved with the item. */
    val manualMetricsJson: String = "[]"
)

@Serializable
@Entity(
    tableName = "nested_item_tags",
    primaryKeys = ["itemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = NestedListItemEntity::class,
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
data class NestedItemTagEntity(
    val itemId: String,
    val tagId: String
)

@Serializable
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String = "",
    val reminderMinutes: Int? = null,
    val sortOrder: Int = 0,
    /** Inline JSON list of RoutineStepTemplate; history keeps percent only. */
    val stepsJson: String = "[]",
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /** True when the row changed locally since the last successful upload. */
    val dirty: Boolean = true,
    /** Tombstone: true once the item is deleted locally; purged after upload. */
    val deleted: Boolean = false,
)

@Serializable
@Entity(
    tableName = "routine_logs",
    primaryKeys = ["routineId", "dateEpochDays"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("dateEpochDays")]
)
data class RoutineLogEntity(
    val routineId: String,
    val dateEpochDays: Int,
    /** Completion percent 0..100 for the day; drives heatmap intensity. */
    val percent: Int,
    val updatedAtMillis: Long,
    val dirty: Boolean = true,
    val deleted: Boolean = false
)

@Database(
    entities = [
        TaskEntity::class,
        SubTaskEntity::class,
        NoteEntity::class,
        DailyPlanItemEntity::class,
        PeriodGoalEntity::class,
        TagEntity::class,
        TaskTagEntity::class,
        NoteTagEntity::class,
        DailyPlanItemTagEntity::class,
        JournalEntryEntity::class,
        JournalEntryTagEntity::class,
        TaskReminderEntity::class,
        TaskFilterEntity::class,
        ListEntity::class,
        ListSectionEntity::class,
        TaskListEntity::class,
        NoteListEntity::class,
        NestedDocumentEntity::class,
        NestedListItemEntity::class,
        NestedItemTagEntity::class,
        DailyReflectStatsEntity::class,
        DailyTagRollupEntity::class,
        HabitDailyRollupEntity::class,
        RoutineEntity::class,
        RoutineLogEntity::class,
        QuickNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
@ConstructedBy(CheckItDatabaseConstructor::class)
abstract class CheckItDatabase : RoomDatabase() {
    abstract fun checkItDao(): CheckItDao
    abstract fun quickNoteDao(): QuickNoteDao
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
                seedDefaultData(connection)
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
            INSERT INTO task_filters(id, name, icon, color, tagId, dueDatePreset, status, priority, includeTrashed, sortOrder)
            SELECT '${filter.id}', '${filter.name}', '${filter.icon}', '${filter.color}', NULL, $dueDatePreset, $status, $priority, $includeTrashed, ${filter.sortOrder}
            WHERE NOT EXISTS(SELECT 1 FROM task_filters WHERE id = '${filter.id}')
            """.trimIndent()
        )
    }
}

private fun seedDefaultData(connection: SQLiteConnection) {
    val now = Clock.System.now().toEpochMilliseconds()
    connection.execSQL(
        """
        INSERT INTO lists(id, title, icon, color, sortOrder, isArchived, createdAtMillis, updatedAtMillis, dirty, deleted)
        SELECT 'inbox', 'Inbox', 'Inbox', '#2563EB', 0, 0, $now, $now, 0, 0
        WHERE NOT EXISTS(SELECT 1 FROM lists WHERE id = 'inbox')
        """.trimIndent()
    )
}

private data class TaskFilterSeed(
    val id: String,
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
    TaskFilterSeed(id = "all", name = "All", icon = "AllInclusive", color = "#475569", sortOrder = 0),
    TaskFilterSeed(
        id = "today",
        name = "Today",
        icon = "Today",
        color = "#2563EB",
        dueDatePreset = "Today",
        sortOrder = 1
    ),
    TaskFilterSeed(
        id = "upcoming",
        name = "Upcoming",
        icon = "Schedule",
        color = "#0891B2",
        dueDatePreset = "Upcoming",
        sortOrder = 2
    ),
    TaskFilterSeed(
        id = "overdue",
        name = "Overdue",
        icon = "Flag",
        color = "#EA580C",
        dueDatePreset = "Overdue",
        sortOrder = 3
    ),
    TaskFilterSeed(
        id = "no-date",
        name = "No date",
        icon = "Schedule",
        color = "#7C3AED",
        dueDatePreset = "NoDate",
        sortOrder = 4
    ),
    TaskFilterSeed(
        id = "completed",
        name = "Completed",
        icon = "TaskAlt",
        color = "#059669",
        status = "Completed",
        sortOrder = 5
    ),
    TaskFilterSeed(
        id = "high-priority",
        name = "High priority",
        icon = "PriorityHigh",
        color = "#DC2626",
        priority = "High",
        sortOrder = 6
    ),
    TaskFilterSeed(id = "trashed", name = "Trashed", icon = "Delete", color = "#6B7280", includeTrashed = true, sortOrder = 7)
)

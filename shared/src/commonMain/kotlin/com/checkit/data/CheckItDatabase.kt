package com.checkit.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
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

@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val icon: String,
    val color: String,
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
    indices = [
        Index("status"),
        Index("priority"),
        Index("doDateEpochDays")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
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
    val trashedAtMillis: Long? = null
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
    indices = []
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val content: String,
    val status: String = "Open",
    val dateEpochDays: Int? = null,
    val startTimeMinutes: Int? = null,
    val createdAtMillis: Long,
    val editedAtMillis: Long,
    val label: String? = null,
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
        ),
        ForeignKey(
            entity = NestedListItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["nestedListItemId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("dateEpochDays"), Index("taskId"), Index("status"), Index("nestedListItemId")]
)
data class DailyPlanItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateEpochDays: Int,
    val taskId: Long? = null,
    val nestedListItemId: Long? = null,
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
    val carriedFromItemId: Long? = null,
    /** Timestamp (epoch millis) when this item was resolved by a review or carry-over. */
    val handledAtMillis: Long? = null
)

@Entity(
    tableName = "period_goals",
    indices = [Index(value = ["periodType", "startEpochDays"], unique = true)]
)
data class PeriodGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val periodType: String,
    val startEpochDays: Int,
    val endEpochDays: Int,
    val review: String = "",
    val goal: String? = null,
    /** Satisfaction for this period (e.g. 0..5). */
    val ratings: Float = 0f,
    val completedAtMillis: Long? = null,
    val editedAtMillis: Long? = null
)

/**
 * A manually tracked metric attached to a [PeriodGoalEntity], mirroring
 * [NestedManualMetricEntity]: free-form name/value pairs with an optional unit.
 */
@Entity(
    tableName = "period_metrics",
    foreignKeys = [
        ForeignKey(
            entity = PeriodGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId"), Index(value = ["goalId", "sortOrder"])]
)
data class PeriodMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val goalId: Long,
    val name: String,
    val value: String,
    val targetValue: String? = null,
    val unit: String = "None",
    val customUnit: String? = null,
    val sortOrder: Int = 0,
    val enabled: Boolean = true
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
    val tagId: Long,
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
    val tagId: Long,
    val tagName: String,
    val tagColor: String?,
    val doneCount: Int,
    val doneMinutes: Int
)

/** Slim projection of a done daily-plan item for highlights (no tags/labels). */
data class DoneItemSummaryEntity(
    val id: Long,
    val dateEpochDays: Int,
    val title: String,
    val note: String?,
    val source: String,
    val startTimeMinutes: Int?,
    val endTimeMinutes: Int?,
    val completedAtMillis: Long?
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

/** Projection row for per-tag usage counts (tasks, notes, daily plan items, journal entries). */
data class TagUsageCountEntity(
    val tagId: Long,
    val usageCount: Int
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
    val label: String? = null,
    val content: String,
    val moods: String = "",
    val createdTimeMinutes: Int,
    val attachments: String = ""
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
    indices = [Index("listId")]
)
data class ListSectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val listId: Long,
    val title: String,
    val color: String,
    val sortOrder: Int
)

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
    val taskId: Long,
    val listId: Long,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val sectionId: Long? = null
)

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
    val noteId: Long,
    val listId: Long,
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val sectionId: Long? = null
)

@Entity(tableName = "nested_documents")
data class NestedDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

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
    indices = [Index("documentId"), Index("parentId"), Index("startDateEpochDays"), Index("endDateEpochDays"), Index("priority")]
)
data class NestedListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val documentId: Long,
    val parentId: Long? = null,
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
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "nested_manual_metrics",
    foreignKeys = [
        ForeignKey(
            entity = NestedListItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index(value = ["itemId", "sortOrder"])]
)
data class NestedManualMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val itemId: Long,
    val name: String,
    val value: String,
    val targetValue: String? = null,
    val unit: String = "None",
    val customUnit: String? = null,
    val sortOrder: Int = 0,
    val enabled: Boolean = true
)

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
    val itemId: Long,
    val tagId: Long
)

@Database(
    entities = [
        TaskEntity::class,
        SubTaskEntity::class,
        NoteEntity::class,
        DailyPlanItemEntity::class,
        PeriodGoalEntity::class,
        PeriodMetricEntity::class,
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
        NestedManualMetricEntity::class,
        DailyReflectStatsEntity::class,
        DailyTagRollupEntity::class,
        HabitDailyRollupEntity::class
    ],
    version = 10,
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
            INSERT INTO task_filters(name, icon, color, tagId, dueDatePreset, status, priority, includeTrashed, sortOrder)
            SELECT '${filter.name}', '${filter.icon}', '${filter.color}', NULL, $dueDatePreset, $status, $priority, $includeTrashed, ${filter.sortOrder}
            WHERE NOT EXISTS(SELECT 1 FROM task_filters WHERE name = '${filter.name}')
            """.trimIndent()
        )
    }
}

private fun seedDefaultData(connection: SQLiteConnection) {
    connection.execSQL(
        """
        INSERT INTO lists(title, icon, color, sortOrder, isArchived)
        SELECT 'Inbox', 'Inbox', '#2563EB', 0, 0
        WHERE NOT EXISTS(SELECT 1 FROM lists WHERE title = 'Inbox')
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

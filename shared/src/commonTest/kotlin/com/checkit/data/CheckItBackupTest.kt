package com.checkit.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckItBackupTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun backupRoundTripPreservesTables() {
        val backup = CheckItBackup(
            exportedAtMillis = 1_700_000_000_000L,
            settings = UserSettings(languageCode = "vi", themeModeCode = "dark"),
            lists = listOf(ListEntity(id = 1L, title = "Inbox", icon = "Inbox", color = "#2563EB", sortOrder = 0)),
            tags = listOf(TagEntity(id = 2L, name = "focus", color = "#FF0000", sortOrder = 0)),
            tasks = listOf(
                TaskEntity(
                    id = 3L,
                    name = "Write backup",
                    status = "Open",
                    priority = "High",
                    createdAtMillis = 10L,
                    updatedAtMillis = 20L,
                )
            ),
            taskTags = listOf(TaskTagEntity(taskId = 3L, tagId = 2L)),
            taskLists = listOf(TaskListEntity(taskId = 3L, listId = 1L)),
            quickNotes = listOf(
                QuickNoteEntity(
                    id = "note-1",
                    content = "hello",
                    status = "NEXT",
                    createdAt = 10L,
                    updatedAt = 20L,
                    sortOrder = 1.0,
                    remindAt = null,
                    deleteAt = null,
                    deleted = false,
                )
            ),
        )

        val encoded = json.encodeToString(CheckItBackup.serializer(), backup)
        val decoded = json.decodeFromString(CheckItBackup.serializer(), encoded)

        assertEquals(CheckItBackup.BACKUP_VERSION, decoded.version)
        assertEquals(backup.exportedAtMillis, decoded.exportedAtMillis)
        assertEquals(backup.settings, decoded.settings)
        assertEquals(backup.lists, decoded.lists)
        assertEquals(backup.tags, decoded.tags)
        assertEquals(backup.tasks, decoded.tasks)
        assertEquals(backup.taskTags, decoded.taskTags)
        assertEquals(backup.taskLists, decoded.taskLists)
        assertEquals(backup.quickNotes, decoded.quickNotes)
    }

    @Test
    fun backupDecodesWithUnknownKeys() {
        val decoded = json.decodeFromString<CheckItBackup>("""{"version":1,"unknownField":true}""")
        assertEquals(1, decoded.version)
        assertEquals(emptyList(), decoded.tasks)
    }
}

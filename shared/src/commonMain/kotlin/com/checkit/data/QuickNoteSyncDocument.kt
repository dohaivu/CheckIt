package com.checkit.data

import com.checkit.domain.QuickNote
import com.checkit.domain.QuickNoteStatus
import com.checkit.domain.QuickNoteType

/**
 * Firestore document mapping for `users/{userId}/quickNotes/{quickNoteId}`.
 *
 * Uses plain `Map<String, Any?>` so it stays platform-agnostic and unit
 * testable; the Android implementation passes these maps straight to
 * Firestore. Field names match the feature plan's example document.
 */
object QuickNoteSyncDocument {
    const val FIELD_ID = "id"
    const val FIELD_CONTENT = "content"
    const val FIELD_STATUS = "status"
    const val FIELD_CREATED_AT = "createdAt"
    const val FIELD_UPDATED_AT = "updatedAt"
    const val FIELD_SORT_ORDER = "sortOrder"
    const val FIELD_REMIND_AT = "remindAt"
    const val FIELD_DELETE_AT = "deleteAt"
    const val FIELD_DELETED = "deleted"
    const val FIELD_TYPE = "type"
    const val FIELD_ATTACHMENT_URL = "attachmentUrl"

    fun toMap(note: QuickNote): Map<String, Any?> = mapOf(
        FIELD_ID to note.id,
        FIELD_CONTENT to note.content,
        FIELD_STATUS to note.status.name,
        FIELD_CREATED_AT to note.createdAt,
        FIELD_UPDATED_AT to note.updatedAt,
        FIELD_SORT_ORDER to note.sortOrder,
        FIELD_REMIND_AT to note.remindAt,
        FIELD_DELETE_AT to note.deleteAt,
        FIELD_DELETED to note.deleted,
        FIELD_TYPE to note.type.name,
        FIELD_ATTACHMENT_URL to note.attachmentUrl,
    )

    /** Returns null when the document is missing required fields. */
    fun fromMap(documentId: String, map: Map<String, Any?>?): QuickNote? {
        if (map == null) return null
        val content = map[FIELD_CONTENT] as? String ?: return null
        val createdAt = (map[FIELD_CREATED_AT] as? Number)?.toLong() ?: return null
        val updatedAt = (map[FIELD_UPDATED_AT] as? Number)?.toLong() ?: return null
        val sortOrder = (map[FIELD_SORT_ORDER] as? Number)?.toDouble() ?: return null
        val status = (map[FIELD_STATUS] as? String)
            ?.let { runCatching { QuickNoteStatus.valueOf(it) }.getOrNull() }
            ?: QuickNoteStatus.NEXT
        val type = (map[FIELD_TYPE] as? String)
            ?.let { runCatching { QuickNoteType.valueOf(it) }.getOrNull() }
            ?: QuickNoteType.TEXT
        return QuickNote(
            id = (map[FIELD_ID] as? String)?.takeIf { it.isNotBlank() } ?: documentId,
            content = content,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            sortOrder = sortOrder,
            remindAt = (map[FIELD_REMIND_AT] as? Number)?.toLong(),
            deleteAt = (map[FIELD_DELETE_AT] as? Number)?.toLong(),
            deleted = map[FIELD_DELETED] as? Boolean ?: false,
            type = type,
            attachmentLocalPath = null,
            attachmentUrl = map[FIELD_ATTACHMENT_URL] as? String,
        )
    }

    /**
     * Last-write-wins on [QuickNote.updatedAt]: returns the remote note when
     * it should replace the local one, null when local wins or is equal.
     * A missing local note always accepts remote (including tombstones).
     */
    fun resolveLocal(local: QuickNote?, remote: QuickNote): QuickNote? =
        if (local == null || remote.updatedAt > local.updatedAt) remote else null
}

package com.checkit.domain

import kotlin.time.Clock
import kotlin.uuid.Uuid

enum class QuickNoteStatus {
    NEXT,
    TO_BE_DELETED,
}

enum class QuickNoteType {
    TEXT,
    IMAGE,
    AUDIO,
    VIDEO,
}

data class QuickNote(
    val id: String,
    val content: String,
    val status: QuickNoteStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Double,
    val remindAt: Long?,
    val deleteAt: Long?,
    val deleted: Boolean,
    val type: QuickNoteType = QuickNoteType.TEXT,
    /** Device-local file path; never synced. */
    val attachmentLocalPath: String? = null,
    /** Remote download URL; synced via the sync document. */
    val attachmentUrl: String? = null,
    /** Only None or High are used; other values normalize to None on read. */
    val priority: TaskPriority = TaskPriority.None,
) {
    val isActive: Boolean get() = !deleted
    val isExpired: Boolean get() = QuickNoteRules.isExpired(this, Clock.System.now().toEpochMilliseconds())
}

object QuickNoteRules {
    const val DELETE_AFTER_MILLIS = 24L * 60L * 60L * 1000L
    /** Untouched NEXT notes older than this are auto-moved to TO_BE_DELETED. */
    const val INACTIVITY_AFTER_MILLIS = 24L * 60L * 60L * 1000L
    /** Tombstones older than this (and confirmed uploaded) are purged for good. */
    const val PURGE_AFTER_MILLIS = 7L * 24L * 60L * 60L * 1000L
    const val REMINDER_15_MIN_MILLIS = 15L * 60L * 1000L
    const val REMINDER_30_MIN_MILLIS = 30L * 60L * 1000L
    const val REMINDER_1_HOUR_MILLIS = 60L * 60L * 1000L
    const val SORT_GAP = 1000.0
    const val SORT_MIN_GAP = 1e-6

    fun newNote(
        content: String,
        now: Long,
        bottomSortOrder: Double?,
        id: String = Uuid.random().toString(),
        type: QuickNoteType = QuickNoteType.TEXT,
        attachmentLocalPath: String? = null,
    ): QuickNote {
        val trimmed = content.trim()
        require(trimmed.isNotBlank()) { "Quick note content must not be blank" }
        return QuickNote(
            id = id,
            content = trimmed,
            status = QuickNoteStatus.NEXT,
            createdAt = now,
            updatedAt = now,
            sortOrder = (bottomSortOrder ?: 0.0) + SORT_GAP,
            remindAt = null,
            deleteAt = null,
            deleted = false,
            type = type,
            attachmentLocalPath = attachmentLocalPath,
            attachmentUrl = null,
        )
    }

    fun moveToBeDeleted(note: QuickNote, now: Long): QuickNote =
        note.copy(
            status = QuickNoteStatus.TO_BE_DELETED,
            deleteAt = now + DELETE_AFTER_MILLIS,
            remindAt = null,
            updatedAt = now,
        )

    fun setReminder(note: QuickNote, remindAt: Long?, now: Long): QuickNote {
        if (remindAt != null) {
            require(remindAt > now) { "Reminder must be in the future" }
        }
        return note.copy(remindAt = remindAt, updatedAt = now)
    }

    /** QuickNote only uses None/High; anything else normalizes to None. */
    fun coercePriority(priority: TaskPriority): TaskPriority =
        if (priority == TaskPriority.High) TaskPriority.High else TaskPriority.None

    fun reminderAt(now: Long, durationMillis: Long): Long = now + durationMillis

    fun isExpired(note: QuickNote, now: Long): Boolean {
        if (note.deleted) return true
        if (note.status != QuickNoteStatus.TO_BE_DELETED) return false
        val deleteAt = note.deleteAt ?: return false
        return deleteAt <= now
    }

    fun markDeleted(note: QuickNote, now: Long): QuickNote =
        note.copy(deleted = true, updatedAt = now)

    fun restore(note: QuickNote, now: Long, bottomSortOrder: Double?): QuickNote =
        note.copy(
            status = QuickNoteStatus.NEXT,
            deleteAt = null,
            remindAt = null,
            updatedAt = now,
            sortOrder = (bottomSortOrder ?: 0.0) + SORT_GAP,
            deleted = false
        )

    fun clearReminder(note: QuickNote, now: Long): QuickNote =
        if (note.remindAt == null) note else note.copy(remindAt = null, updatedAt = now)

    fun sortOrderBetween(before: Double?, after: Double?): Double =
        when {
            before == null && after == null -> SORT_GAP
            before == null -> after!! - SORT_GAP
            after == null -> before + SORT_GAP
            else -> (before + after) / 2.0
        }

    fun needsNormalization(notes: List<QuickNote>): Boolean {
        if (notes.size < 2) return false
        val sorted = notes.sortedBy { it.sortOrder }
        for (i in 1 until sorted.size) {
            if (sorted[i].sortOrder - sorted[i - 1].sortOrder < SORT_MIN_GAP) return true
        }
        return false
    }

    fun normalizedOrders(notes: List<QuickNote>): Map<String, Double> =
        notes.sortedBy { it.sortOrder }
            .mapIndexed { index, note -> note.id to ((index + 1) * SORT_GAP) }
            .toMap()

    /** Last-write-wins: true when remote should replace local. */
    fun shouldAcceptRemote(local: QuickNote, remote: QuickNote): Boolean =
        remote.updatedAt > local.updatedAt

    fun remainingMillis(note: QuickNote, now: Long): Long? {
        val deleteAt = note.deleteAt ?: return null
        return (deleteAt - now).coerceAtLeast(0L)
    }
}

package com.checkit.data

import com.checkit.domain.QuickNote
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock

/**
 * Swift-friendly Room access for the macOS Firestore sync implemented in
 * Swift (`QuickNoteFirestoreSync`). All functions are suspending so Swift
 * calls them through completion handlers wrapped in continuations.
 *
 * Document mapping stays single-sourced here via [QuickNoteSyncDocument]:
 * Swift passes remote documents as JSON strings and receives nothing but
 * domain objects, ids, and booleans back.
 */
class QuickNoteSyncBridge(
    private val dao: QuickNoteDao,
) {
    suspend fun dirtyNotes(): List<QuickNote> =
        dao.getDirty().map { it.toDomain() }

    suspend fun setAttachmentUrl(id: String, url: String) {
        dao.setAttachmentUrl(id, url, Clock.System.now().toEpochMilliseconds())
    }

    suspend fun setAttachmentLocalPath(id: String, path: String?) {
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(attachmentLocalPath = path))
    }

    suspend fun markClean(ids: List<String>, maxUpdatedAt: Long) {
        dao.markClean(ids, maxUpdatedAt)
    }

    suspend fun noteById(id: String): QuickNote? =
        dao.getById(id)?.toDomain()

    /**
     * Last-write-wins merge of one remote document (JSON-encoded
     * [QuickNoteSyncDocument]) into Room. Returns true when the remote won
     * and was applied with dirty = false.
     */
    suspend fun applyRemoteJson(json: String): Boolean {
        val element = runCatching { Json.parseToJsonElement(json) }
            .getOrNull()?.jsonObject ?: return false
        val remote = QuickNoteSyncDocument.fromMap(
            documentId = element[QuickNoteSyncDocument.FIELD_ID]
                ?.jsonPrimitive?.contentOrNull ?: "",
            map = element.mapValues { it.value.toSyncValue() },
        ) ?: return false
        val existing = dao.getById(remote.id)?.toDomain()
        val winner = QuickNoteSyncDocument.resolveLocal(existing, remote) ?: return false
        // Keep the local file when the URL is unchanged, like Android's
        // download cache check (the bytes themselves arrive from Swift).
        val localPath = existing?.attachmentLocalPath
            ?.takeIf { it.isNotBlank() && existing.attachmentUrl == winner.attachmentUrl }
        dao.upsert(winner.copy(attachmentLocalPath = localPath).toEntity(dirty = false))
        return true
    }

    suspend fun purgeableTombstones(cutoffMillis: Long): List<QuickNote> =
        dao.getPurgeableTombstones(cutoffMillis).map { it.toDomain() }

    suspend fun hardDeleteNotes(ids: List<String>) {
        dao.hardDelete(ids)
    }

    private fun JsonElement.toSyncValue(): Any? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> when {
            isString -> contentOrNull
            booleanOrNull != null -> booleanOrNull
            longOrNull != null -> longOrNull
            else -> doubleOrNull
        }
        else -> null
    }
}

package com.checkit.data

import com.checkit.domain.QuickNote
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
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

    /**
     * JSON-encoded push documents for dirty rows, mapped with
     * [QuickNoteSyncDocument] so field names stay single-sourced.
     * Platforms parse each element back to a native dictionary for upload.
     */
    suspend fun dirtyNoteDocuments(): List<String> =
        dao.getDirty().map { toJson(QuickNoteSyncDocument.toMap(it.toDomain())) }

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
     *
     * @param downloadedAttachmentPath device-local path the platform just
     * downloaded for this document's attachment URL, or null when no fresh
     * download happened. Passed through only when the winner carries an
     * attachment URL; otherwise the existing cached path is kept when the
     * URL is unchanged (display fallback, like Android's download logic).
     */
    suspend fun applyRemoteJson(json: String, downloadedAttachmentPath: String?): Boolean {
        val element = runCatching { Json.parseToJsonElement(json) }
            .getOrNull()?.jsonObject ?: return false
        val remote = QuickNoteSyncDocument.fromMap(
            documentId = element[QuickNoteSyncDocument.FIELD_ID]
                ?.jsonPrimitive?.contentOrNull ?: "",
            map = element.mapValues { it.value.toSyncValue() },
        ) ?: return false
        val existing = dao.getById(remote.id)?.toDomain()
        val winner = QuickNoteSyncDocument.resolveLocal(existing, remote) ?: return false
        val localPath = when {
            downloadedAttachmentPath != null && winner.attachmentUrl != null ->
                downloadedAttachmentPath
            else -> existing?.attachmentLocalPath
                ?.takeIf { it.isNotBlank() && existing.attachmentUrl == winner.attachmentUrl }
        }
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

    private fun toJson(map: Map<String, Any?>): String = buildJsonObject {
        map.forEach { (key, value) ->
            when (value) {
                null -> put(key, JsonNull)
                is String -> put(key, value)
                is Boolean -> put(key, value)
                is Number ->
                    if (value is Double || value is Float) put(key, value.toDouble())
                    else put(key, value.toLong())
                else -> put(key, value.toString())
            }
        }
    }.toString()
}

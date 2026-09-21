package com.checkit.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Firestore document mapping for nested lists:
 * `users/{userId}/nestedDocuments/{documentId}` plus the `nestedItems`
 * subcollection per document.
 *
 * Uses plain `Map<String, Any?>` so it stays platform-agnostic and unit
 * testable; both platforms convert to/from JSON strings at the bridge
 * boundary so the merge logic is implemented exactly once.
 *
 * Tag membership syncs embedded as [FIELD_TAG_IDS]; the join rows stay
 * local-only and are rebuilt from the ids on apply (unknown ids are
 * dropped to satisfy the tags foreign key).
 */
object NestedSyncDocument {
    const val FIELD_ID = "id"
    const val FIELD_TITLE = "title"
    const val FIELD_CREATED_AT = "createdAtMillis"
    const val FIELD_UPDATED_AT = "updatedAtMillis"
    const val FIELD_DELETED = "deleted"

    const val FIELD_DOCUMENT_ID = "documentId"
    const val FIELD_PARENT_ID = "parentId"
    const val FIELD_POSITION = "position"
    const val FIELD_TEXT = "text"
    const val FIELD_NOTE = "note"
    const val FIELD_CHECKBOX_ENABLED = "checkboxEnabled"
    const val FIELD_CHECKED = "checked"
    const val FIELD_COLLAPSED = "collapsed"
    const val FIELD_TEXT_STYLE = "textStyle"
    const val FIELD_TEXT_COLOR = "textColor"
    const val FIELD_BACKGROUND_COLOR = "backgroundColor"
    const val FIELD_START_DATE = "startDateEpochDays"
    const val FIELD_END_DATE = "endDateEpochDays"
    const val FIELD_PRIORITY = "priority"
    const val FIELD_ACTUAL_MINUTES = "actualMinutes"
    const val FIELD_ROLLUP_POLICY = "metricRollupPolicy"
    const val FIELD_SHOW_TRACKED = "showTrackedMinutes"
    const val FIELD_PROGRESS = "progressPercent"
    const val FIELD_MANUAL_METRICS = "manualMetricsJson"
    const val FIELD_TAG_IDS = "tagIds"

    fun docToMap(
        id: String,
        title: String,
        createdAtMillis: Long,
        updatedAtMillis: Long,
        deleted: Boolean,
    ): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_TITLE to title,
        FIELD_CREATED_AT to createdAtMillis,
        FIELD_UPDATED_AT to updatedAtMillis,
        FIELD_DELETED to deleted,
    )

    fun itemToMap(
        id: String,
        documentId: String,
        parentId: String?,
        position: Int,
        text: String,
        note: String?,
        checkboxEnabled: Boolean,
        checked: Boolean,
        collapsed: Boolean,
        textStyle: String,
        textColor: String,
        backgroundColor: String,
        startDateEpochDays: Int?,
        endDateEpochDays: Int?,
        priority: String,
        actualMinutes: Int,
        metricRollupPolicy: String,
        showTrackedMinutes: Boolean,
        progressPercent: Int?,
        manualMetricsJson: String,
        tagIds: List<String>,
        createdAtMillis: Long,
        updatedAtMillis: Long,
        deleted: Boolean,
    ): Map<String, Any?> = mapOf(
        FIELD_ID to id,
        FIELD_DOCUMENT_ID to documentId,
        FIELD_PARENT_ID to parentId,
        FIELD_POSITION to position,
        FIELD_TEXT to text,
        FIELD_NOTE to note,
        FIELD_CHECKBOX_ENABLED to checkboxEnabled,
        FIELD_CHECKED to checked,
        FIELD_COLLAPSED to collapsed,
        FIELD_TEXT_STYLE to textStyle,
        FIELD_TEXT_COLOR to textColor,
        FIELD_BACKGROUND_COLOR to backgroundColor,
        FIELD_START_DATE to startDateEpochDays,
        FIELD_END_DATE to endDateEpochDays,
        FIELD_PRIORITY to priority,
        FIELD_ACTUAL_MINUTES to actualMinutes,
        FIELD_ROLLUP_POLICY to metricRollupPolicy,
        FIELD_SHOW_TRACKED to showTrackedMinutes,
        FIELD_PROGRESS to progressPercent,
        FIELD_MANUAL_METRICS to manualMetricsJson,
        FIELD_TAG_IDS to tagIds,
        FIELD_CREATED_AT to createdAtMillis,
        FIELD_UPDATED_AT to updatedAtMillis,
        FIELD_DELETED to deleted,
    )

    /** Returns null when the document is missing required fields. */
    fun docFromMap(documentId: String, map: Map<String, Any?>?): RemoteNestedDocument? {
        if (map == null) return null
        val createdAt = (map[FIELD_CREATED_AT] as? Number)?.toLong() ?: return null
        val updatedAt = (map[FIELD_UPDATED_AT] as? Number)?.toLong() ?: return null
        return RemoteNestedDocument(
            id = (map[FIELD_ID] as? String)?.takeIf { it.isNotBlank() } ?: documentId,
            title = map[FIELD_TITLE] as? String ?: "",
            createdAtMillis = createdAt,
            updatedAtMillis = updatedAt,
            deleted = map[FIELD_DELETED] as? Boolean ?: false,
        )
    }

    /** Returns null when the document is missing required fields. */
    fun itemFromMap(documentId: String, map: Map<String, Any?>?): RemoteNestedItem? {
        if (map == null) return null
        val createdAt = (map[FIELD_CREATED_AT] as? Number)?.toLong() ?: return null
        val updatedAt = (map[FIELD_UPDATED_AT] as? Number)?.toLong() ?: return null
        val text = map[FIELD_TEXT] as? String ?: return null
        return RemoteNestedItem(
            id = (map[FIELD_ID] as? String)?.takeIf { it.isNotBlank() } ?: return null,
            documentId = (map[FIELD_DOCUMENT_ID] as? String)?.takeIf { it.isNotBlank() } ?: documentId,
            parentId = (map[FIELD_PARENT_ID] as? String)?.takeIf { it.isNotBlank() },
            position = (map[FIELD_POSITION] as? Number)?.toInt() ?: 0,
            text = text,
            note = map[FIELD_NOTE] as? String,
            checkboxEnabled = map[FIELD_CHECKBOX_ENABLED] as? Boolean ?: false,
            checked = map[FIELD_CHECKED] as? Boolean ?: false,
            collapsed = map[FIELD_COLLAPSED] as? Boolean ?: false,
            textStyle = map[FIELD_TEXT_STYLE] as? String ?: "Body",
            textColor = map[FIELD_TEXT_COLOR] as? String ?: "Default",
            backgroundColor = map[FIELD_BACKGROUND_COLOR] as? String ?: "Default",
            startDateEpochDays = (map[FIELD_START_DATE] as? Number)?.toInt(),
            endDateEpochDays = (map[FIELD_END_DATE] as? Number)?.toInt(),
            priority = map[FIELD_PRIORITY] as? String ?: "None",
            actualMinutes = (map[FIELD_ACTUAL_MINUTES] as? Number)?.toInt() ?: 0,
            metricRollupPolicy = map[FIELD_ROLLUP_POLICY] as? String ?: "IncludeChildren",
            showTrackedMinutes = map[FIELD_SHOW_TRACKED] as? Boolean ?: false,
            progressPercent = (map[FIELD_PROGRESS] as? Number)?.toInt(),
            manualMetricsJson = map[FIELD_MANUAL_METRICS] as? String ?: "[]",
            tagIds = (map[FIELD_TAG_IDS] as? List<*>).orEmpty().filterIsInstance<String>(),
            createdAtMillis = createdAt,
            updatedAtMillis = updatedAt,
            deleted = map[FIELD_DELETED] as? Boolean ?: false,
        )
    }

    /**
     * Last-write-wins on `updatedAtMillis`: true when the remote should
     * replace the local row. A missing local row always accepts remote
     * (including tombstones); equal timestamps keep local.
     */
    fun shouldApplyRemote(localUpdatedAt: Long?, remoteUpdatedAt: Long): Boolean =
        localUpdatedAt == null || remoteUpdatedAt > localUpdatedAt

    /** Encodes a push/pull map for the bridge boundary (both platforms). */
    fun toJson(map: Map<String, Any?>): String = buildJsonObject {
        map.forEach { (key, value) ->
            when (value) {
                null -> put(key, JsonNull)
                is String -> put(key, value)
                is Boolean -> put(key, value)
                is Number ->
                    if (value is Double || value is Float) put(key, value.toDouble())
                    else put(key, value.toLong())
                is List<*> -> put(
                    key,
                    JsonArray(value.map {
                        when (it) {
                            null -> JsonNull
                            is String -> JsonPrimitive(it)
                            is Boolean -> JsonPrimitive(it)
                            is Number -> JsonPrimitive(it.toLong())
                            else -> JsonPrimitive(it.toString())
                        }
                    })
                )
                else -> put(key, value.toString())
            }
        }
    }.toString()

    /** Decodes a bridge JSON string back to a map; null on invalid input. */
    fun mapFromJson(json: String): Map<String, Any?>? =
        runCatching { Json.parseToJsonElement(json).jsonObject }
            .getOrNull()
            ?.mapValues { it.value.toSyncValue() }

    private fun JsonElement.toSyncValue(): Any? = when (this) {
        is JsonNull -> null
        is JsonArray -> map { it.toSyncValue() }
        is JsonPrimitive -> when {
            isString -> contentOrNull
            booleanOrNull != null -> booleanOrNull
            longOrNull != null || intOrNull != null -> longOrNull ?: intOrNull?.toLong()
            else -> doubleOrNull
        }
        else -> null
    }
}

/** Sync view of one nested document row (detached from Room/domain). */
data class RemoteNestedDocument(
    val id: String,
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deleted: Boolean,
)

/** Sync view of one nested item row, tags embedded (detached from Room/domain). */
data class RemoteNestedItem(
    val id: String,
    val documentId: String,
    val parentId: String?,
    val position: Int,
    val text: String,
    val note: String?,
    val checkboxEnabled: Boolean,
    val checked: Boolean,
    val collapsed: Boolean,
    val textStyle: String,
    val textColor: String,
    val backgroundColor: String,
    val startDateEpochDays: Int?,
    val endDateEpochDays: Int?,
    val priority: String,
    val actualMinutes: Int,
    val metricRollupPolicy: String,
    val showTrackedMinutes: Boolean,
    val progressPercent: Int?,
    val manualMetricsJson: String,
    val tagIds: List<String>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deleted: Boolean,
)

package com.checkit.domain

/**
 * Plans a copy of [itemId] as a new sibling placed directly after the source.
 *
 * Only the source row is copied; descendants are intentionally excluded. The
 * copy keeps the source's content, formatting, tags and metric configuration,
 * while execution state is reset so the duplicate behaves like a fresh node:
 * it is unchecked, has no completion timestamp or tracked minutes, and starts
 * uncollapsed.
 *
 * Existing siblings are not resequenced. The duplicate simply takes
 * `source.position + 1`; callers that need strict ordering must handle ties
 * themselves.
 *
 * Returns null when [itemId] is not present in [items].
 *
 * [dateOffsetDays] shifts both bounds of the copy after execution state is
 * reset (open-ended ranges stay open-ended). [clearDates] wins over the
 * offset and removes both bounds, so "duplicate without dates" is a one-shot
 * action with no lingering date content.
 */
fun planNestedItemDuplicate(
    items: List<NestedListItem>,
    itemId: String,
    newId: String,
    nowMillis: Long,
    dateOffsetDays: Int = 0,
    clearDates: Boolean = false
): NestedListItem? {
    val source = items.firstOrNull { it.id == itemId } ?: return null
    val shifted = if (clearDates) source.withoutDates() else source.shiftedDatesBy(dateOffsetDays)
    return shifted.copy(
        id = newId,
        parentId = source.parentId,
        position = source.position + 1,
        checked = false,
        completedAtMillis = null,
        collapsed = false,
        actualMinutes = 0,
        createdAtMillis = nowMillis,
        updatedAtMillis = nowMillis
    )
}

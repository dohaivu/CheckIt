package com.checkit.domain

import kotlinx.datetime.LocalDate

/**
 * Plain domain and read-model types for nested lists.
 *
 * Tree building lives in `NestedTree.kt`, filtering in `NestedFilter.kt`,
 * move planning in `NestedMoves.kt`, and widget focus in
 * `NestedFocusWidgets.kt`.
 */

enum class NestedTextStyle {
    Body,
    Header,
    Subheader
}

enum class NestedColorToken {
    Default,
    Red,
    Orange,
    Yellow,
    Green,
    Teal,
    Cyan,
    Blue,
    Indigo,
    Purple,
    Pink,
    Brown,
    Gray;

    /**
     * Hex RGB for concrete tokens; null for [Default], which follows the
     * theme. Single source of truth so the Compose and macOS SwiftUI palettes
     * cannot drift.
     */
    fun hex(): String? = when (this) {
        Default -> null
        Red -> "#E57373"
        Orange -> "#FFB74D"
        Yellow -> "#FFD54F"
        Green -> "#66BB6A"
        Teal -> "#26A69A"
        Cyan -> "#26C6DA"
        Blue -> "#42A5F5"
        Indigo -> "#5C6BC0"
        Purple -> "#AB47BC"
        Pink -> "#EC407A"
        Brown -> "#8D6E63"
        Gray -> "#78909C"
    }
}

/**
 * Shape of a nested row's card. `None` is not needed: [NestedBorderStyle.None]
 * hides the stroke while the shape still defines the background fill.
 */
enum class NestedBorderShape {
    Rectangle,
    Rounded,
    Pill,
    Cloud,
    Ticket,
    CutCorner,
    Hexagon,
    Leaf;

    /**
     * Menu label. The enum name is the persisted value, so this is
     * presentational only and shared by both platforms.
     */
    fun displayName(): String = when (this) {
        CutCorner -> "Cut corner"
        else -> name
    }
}

/** Stroke style for a nested row's border. `None` keeps the card unstroked. */
enum class NestedBorderStyle {
    None,
    Solid,
    Dashed,
    Dotted
}

enum class MetricRollupPolicy {
    IncludeChildren,
    OwnOnly,
    ExcludeFromParent
}

data class NestedMetricSummary(
    /** Direct children with a checkbox that are checked. */
    val doneTaskCount: Int = 0,
    /** Direct children with a checkbox enabled, checked or not. */
    val totalTaskCount: Int = 0,
    val trackedMinutes: Int = 0
)

/**
 * A single nested-lists document. Holds one unlimited-depth item tree.
 */
data class NestedDocument(
    val id: String = "",
    val title: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

/**
 * One node of a [NestedDocument] tree. Uniform line; checkbox style is per-item.
 * [parentId] null means the item sits at the document root level.
 */
data class NestedListItem(
    val id: String = "",
    val documentId: String,
    val parentId: String? = null,
    val position: Int,
    val text: String,
    val note: String? = null,
    val label: String? = null,
    val checkboxEnabled: Boolean = false,
    val checked: Boolean = false,
    /** When the item was last checked; null when never checked or unchecked. */
    val completedAtMillis: Long? = null,
    val collapsed: Boolean = false,
    val textStyle: NestedTextStyle = NestedTextStyle.Body,
    val textColor: NestedColorToken = NestedColorToken.Default,
    val backgroundColor: NestedColorToken = NestedColorToken.Default,
    val borderShape: NestedBorderShape = NestedBorderShape.Rounded,
    val borderStyle: NestedBorderStyle = NestedBorderStyle.None,
    val borderColor: NestedColorToken = NestedColorToken.Default,
    /** Optional palette override for this row's dot and owned guide segments. */
    val guideDotColor: NestedColorToken? = null,
    /**
     * Emoji drawn in the row's guide dot instead of the default ring/dot.
     * Null keeps the standard guide. Normalize writes with
     * [sanitizeGuideDot] so a row cannot be used as a free-text field.
     */
    val guideDot: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val priority: TaskPriority = TaskPriority.None,
    val tags: List<TagItem> = emptyList(),
    val actualMinutes: Int = 0,
    val metricRollupPolicy: MetricRollupPolicy = MetricRollupPolicy.IncludeChildren,
    val showTrackedMinutes: Boolean = false,
    /** Manual progress 0..100; null means progress UI is hidden. */
    val progressPercent: Int? = null,
    val manualMetrics: List<MetricItem> = emptyList(),
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

/** UI/read model for one item node with its nested children. */
data class NestedItemNode(
    val item: NestedListItem,
    val children: List<NestedItemNode> = emptyList()
) {
    val hasChildren: Boolean get() = children.isNotEmpty()
}

/**
 * Root of the read model for one document: the document plus its item tree.
 *
 * A regular class rather than a data class: the lazy indexes below are
 * per-instance caches, and a data-class copy would silently reset them.
 */
class NestedDocumentTree(
    val document: NestedDocument,
    val rootNodes: List<NestedItemNode>
) {
    private val index: NestedNodeIndex by lazy { indexNestedNodes(rootNodes) }

    /** All nodes by item id; built once per tree. */
    val nodeById: Map<String, NestedItemNode> get() = index.nodes

    /** All items by id; built in the same pass as [nodeById]. */
    val itemById: Map<String, NestedListItem> get() = index.items

    val flatItems: List<NestedListItem> by lazy { flattenNestedNodes(rootNodes) }
    val metricSummaryById: Map<String, NestedMetricSummary> by lazy { calculateNestedMetricSummaries(rootNodes) }
}

/** Longest guide emoji accepted, in UTF-16 units. */
const val NESTED_GUIDE_DOT_MAX_LENGTH = 16

/**
 * Normalizes a picked guide emoji: trims surrounding whitespace, maps blank to
 * null (so the row falls back to the default dot), and rejects values that are
 * clearly not a single emoji rather than storing arbitrary text.
 *
 * Emojis with skin tones or zero-width joiners need several code points, hence
 * the generous cap; anything longer is treated as invalid input.
 */
fun sanitizeGuideDot(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    return trimmed.takeIf { it.length <= NESTED_GUIDE_DOT_MAX_LENGTH }
}


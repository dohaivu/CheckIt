package com.checkit.domain

import kotlinx.datetime.LocalDate

/**
 * Plain domain and read-model types for nested lists.
 *
 * Tree building lives in `NestedTree.kt`, filtering in `NestedFilter.kt`,
 * move planning in `NestedMoves.kt`, and widget focus in
 * `NestedFocusWidgets.kt`.
 */

/**
 * Row typography. Persisted by [name] in Room and sync payloads; the ordering
 * is the picker order. Legacy `Header`/`Subheader` values are intentionally
 * not migrated and read back as [Body].
 */
enum class NestedTextStyle {
    Title,
    Heading,
    Subheading,
    Body,
    Monostyled
}

/**
 * Concrete row colors shared by nested formatting and `=={cN}==` highlight
 * tokens (see [HighlightPalette]).
 *
 * 13 colors, one hex each. [Default] follows the theme and has no hex.
 * Persisted by [name] in Room and sync payloads, so entries are append-only:
 * renaming or removing one resets existing rows to [Default].
 */
enum class NestedColorToken {
    Default,
    Red,
    Orange,
    Yellow,
    Lime,
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
        Red -> "#DC2626"
        Orange -> "#EA580C"
        Yellow -> "#CA8A04"
        Lime -> "#65A30D"
        Green -> "#16A34A"
        Teal -> "#0D9488"
        Cyan -> "#0891B2"
        Blue -> "#2563EB"
        Indigo -> "#6366F1"
        Purple -> "#7C3AED"
        Pink -> "#DB2777"
        Brown -> "#7C2D12"
        Gray -> "#64748B"
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
    Leaf,
    Ribbon;

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

/**
 * Visual frame for one nested row. Persisted by [name], so entries must never
 * be renamed or removed; an unknown value reads back as [None].
 *
 * This is a style identifier, not a semantic tag. The frame is derived from
 * [spec] so Compose and macOS SwiftUI render the same recipe. Panel, Tab and
 * Rail callouts own the row's card shape, border and tint; Text callouts add
 * only an accented icon and eyebrow, leaving the stored background/border
 * fields in charge. Those stored fields reappear when the callout is removed.
 */
enum class NestedCalloutStyle {
    None,
    Note,
    Tip,
    Idea,
    Example,
    Summary,
    Important,
    Warning,
    Caution,
    Question,
    Quote;

    /** Picker label; [None] is the reset row. */
    fun displayName(): String = when (this) {
        None -> "No callout"
        else -> name
    }

    /** Visual recipe; null for [None]. Shared by Compose and SwiftUI. */
    fun spec(): NestedCalloutSpec? = when (this) {
        None -> null
        Note -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Panel,
            accent = NestedColorToken.Blue,
            shape = NestedBorderShape.Rounded,
            borderStyle = NestedBorderStyle.Solid,
            borderWidthDp = 1f,
            iconKey = "info"
        )
        Tip -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Panel,
            accent = NestedColorToken.Green,
            shape = NestedBorderShape.Leaf,
            borderStyle = NestedBorderStyle.Solid,
            borderWidthDp = 1f,
            iconKey = "lightbulb"
        )
        Idea -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Text,
            accent = NestedColorToken.Yellow,
            shape = null,
            borderStyle = NestedBorderStyle.None,
            borderWidthDp = 1f,
            iconKey = "idea"
        )
        Example -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Panel,
            accent = NestedColorToken.Teal,
            shape = NestedBorderShape.Rounded,
            borderStyle = NestedBorderStyle.Dotted,
            borderWidthDp = 1f,
            iconKey = "sparkles"
        )
        Summary -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Text,
            accent = NestedColorToken.Indigo,
            shape = null,
            borderStyle = NestedBorderStyle.None,
            borderWidthDp = 1f,
            iconKey = "summary"
        )
        Important -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Tab,
            accent = NestedColorToken.Purple,
            shape = NestedBorderShape.CutCorner,
            borderStyle = NestedBorderStyle.Solid,
            borderWidthDp = 1f,
            iconKey = "important"
        )
        Warning -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Tab,
            accent = NestedColorToken.Orange,
            shape = NestedBorderShape.CutCorner,
            borderStyle = NestedBorderStyle.Dashed,
            borderWidthDp = 1f,
            iconKey = "warning"
        )
        Caution -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Tab,
            accent = NestedColorToken.Red,
            shape = NestedBorderShape.Rectangle,
            borderStyle = NestedBorderStyle.Solid,
            borderWidthDp = 2f,
            iconKey = "caution"
        )
        Question -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Rail,
            accent = NestedColorToken.Cyan,
            shape = null,
            borderStyle = NestedBorderStyle.Solid,
            borderWidthDp = 3f,
            iconKey = "question"
        )
        Quote -> NestedCalloutSpec(
            layout = NestedCalloutLayout.Rail,
            accent = NestedColorToken.Gray,
            shape = null,
            borderStyle = NestedBorderStyle.Solid,
            borderWidthDp = 3f,
            iconKey = "quote"
        )
    }
}

/**
 * Keeps a title only while a callout is applied. [NestedCalloutStyle.None] has
 * no header, so a stale title must not survive and reappear when a callout is
 * re-applied later.
 */
fun NestedCalloutStyle.normalizedTitle(title: String?): String? =
    if (this == NestedCalloutStyle.None) null else title?.trim()?.takeIf { it.isNotBlank() }

/** How a callout is laid out around the row. */
enum class NestedCalloutLayout {
    /** Full card with the title as a small eyebrow above the text. */
    Panel,

    /** Full card with the title in a tab attached to the top edge. */
    Tab,

    /** No card: an accent rail with the icon and title inline. */
    Rail,

    /**
     * No preset frame: just the icon and an eyebrow title above the text.
     *
     * The preset contributes typography only, so a text callout stays readable
     * without a card. Explicit row background/border properties still apply
     * when the user sets them.
     */
    Text
}

/** Where the optional title is drawn for a callout layout. */
enum class NestedCalloutTitlePlacement {
    Eyebrow,
    TopTab,
    Inline
}

/** Title treatment implied by a layout; the only source of that mapping. */
fun NestedCalloutLayout.titlePlacement(): NestedCalloutTitlePlacement = when (this) {
    NestedCalloutLayout.Panel,
    NestedCalloutLayout.Text -> NestedCalloutTitlePlacement.Eyebrow
    NestedCalloutLayout.Tab -> NestedCalloutTitlePlacement.TopTab
    NestedCalloutLayout.Rail -> NestedCalloutTitlePlacement.Inline
}

/**
 * One callout's visual recipe. Pure data so both platforms share the look and
 * the contract is unit-testable without a canvas.
 *
 * [shape] is null for [NestedCalloutLayout.Rail], which draws only a left
 * accent rail, and for [NestedCalloutLayout.Text], which has no preset frame.
 * [borderWidthDp] is the stroke width for cards and the rail width for rails.
 * The title treatment is not stored here; it follows from [layout] via
 * [NestedCalloutLayout.titlePlacement].
 */
data class NestedCalloutSpec(
    val layout: NestedCalloutLayout,
    val accent: NestedColorToken,
    val shape: NestedBorderShape?,
    val borderStyle: NestedBorderStyle,
    val borderWidthDp: Float,
    val iconKey: String
)

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
    /** Visual frame; [NestedCalloutStyle.None] renders the plain row. */
    val callout: NestedCalloutStyle = NestedCalloutStyle.None,
    /** Optional override for the callout title; null shows the style name. */
    val calloutTitle: String? = null,
    /** Optional override for the callout layout; null uses callout.spec().layout. */
    val calloutLayout: NestedCalloutLayout? = null,
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
) {
    /**
     * Resolves the frame this row should draw, layering explicit item properties
     * over the [callout] preset. Shared by the Compose row and the SwiftUI row so
     * both platforms render the same frame.
     */
    fun resolveVisuals(): EffectiveRowVisuals {
        val spec = callout.spec()

        // A layout override only means something with a callout; defensive
        // because stored/imported rows can outlive the callout being cleared.
        val layout = spec?.let { calloutLayout ?: it.layout }
        val titlePlacement = layout?.titlePlacement()
        val isTextLayout = layout == NestedCalloutLayout.Text

        val hasBackground = backgroundColor != NestedColorToken.Default
        val bgToken = when {
            hasBackground -> backgroundColor
            // Text carries the accent in the eyebrow, not in a card wash.
            isTextLayout -> NestedColorToken.Default
            spec != null -> spec.accent
            else -> NestedColorToken.Default
        }
        val bgAlpha = when {
            hasBackground -> EffectiveRowVisuals.EXPLICIT_BACKGROUND_ALPHA
            isTextLayout -> 0f
            spec != null -> EffectiveRowVisuals.CALLOUT_BACKGROUND_ALPHA
            else -> 0f
        }

        val borderToken = when {
            borderColor != NestedColorToken.Default -> borderColor
            spec != null -> spec.accent
            else -> NestedColorToken.Default
        }

        val shape = when {
            // Rail draws only a bar, so the frame is always square.
            layout == NestedCalloutLayout.Rail -> NestedBorderShape.Rectangle
            // Text contributes no preset geometry: default to the row's own
            // shape so a decorative preset shape cannot clip the content.
            isTextLayout -> borderShape
            // "Rounded" is the default, so it means "keep the preset shape".
            borderShape != NestedBorderShape.Rounded -> borderShape
            spec?.shape != null -> spec.shape
            else -> borderShape
        }

        val borderStyle = when {
            // "None" is the default, so it means "keep the preset stroke".
            borderStyle != NestedBorderStyle.None -> borderStyle
            // Text has no preset frame; an explicit style above still wins.
            isTextLayout -> NestedBorderStyle.None
            spec != null -> spec.borderStyle
            else -> NestedBorderStyle.None
        }

        val borderWidthDp = spec?.borderWidthDp
            ?: EffectiveRowVisuals.DEFAULT_BORDER_WIDTH_DP

        return EffectiveRowVisuals(
            calloutSpec = spec,
            layout = layout,
            titlePlacement = titlePlacement,
            shape = shape,
            borderStyle = borderStyle,
            borderWidthDp = borderWidthDp,
            borderToken = borderToken,
            bgToken = bgToken,
            bgAlpha = bgAlpha
        )
    }
}

/**
 * Effective visual properties resolved for rendering one row card frame.
 *
 * Explicit item properties ([NestedListItem.backgroundColor],
 * [NestedListItem.borderColor], [NestedListItem.borderShape],
 * [NestedListItem.borderStyle]) override the callout preset so a single callout
 * can be recolored or reshaped per row; the preset supplies the defaults.
 *
 * A [NestedCalloutLayout.Text] callout contributes typography only: no preset
 * stroke and no preset wash, unless the row sets those properties explicitly.
 *
 * The stroke width has no item-level counterpart: it always comes from the
 * preset (or the plain 1.5dp default when the row has no callout).
 */
data class EffectiveRowVisuals(
    val calloutSpec: NestedCalloutSpec?,
    /** Overridden or preset layout; null when the row has no callout. */
    val layout: NestedCalloutLayout?,
    /** Title treatment implied by [layout]; null when the row has no callout. */
    val titlePlacement: NestedCalloutTitlePlacement?,
    val shape: NestedBorderShape,
    val borderStyle: NestedBorderStyle,
    val borderWidthDp: Float,
    /** Explicit border color, else the preset accent. [NestedColorToken.Default] means theme ink. */
    val borderToken: NestedColorToken,
    /** Explicit background, else the preset accent. [NestedColorToken.Default] means no wash. */
    val bgToken: NestedColorToken,
    /** Alpha for [bgToken]: an explicit fill reads stronger than a preset wash. */
    val bgAlpha: Float
) {
    /** Header placement; safe fallback for the callout-less row that draws none. */
    val titlePlacementOrDefault: NestedCalloutTitlePlacement
        get() = titlePlacement ?: NestedCalloutTitlePlacement.Eyebrow

    companion object {
        /** Wash strength for a preset callout tint. */
        const val CALLOUT_BACKGROUND_ALPHA = 0.10f

        /** Wash strength for a row whose background was picked by hand. */
        const val EXPLICIT_BACKGROUND_ALPHA = 0.18f

        /** Plain rows and rows without a callout stroke. */
        const val DEFAULT_BORDER_WIDTH_DP = 1.5f
    }
}

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


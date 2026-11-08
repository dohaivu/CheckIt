package com.checkit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object AppIconColorDefaults {
    const val FallbackColorHex: String = "#64748B"
    val FallbackColor: Color = Color(0xFF64748B)
    val DailyPlanCardColor = FallbackColor

    /**
     * Shared color palette (pickers and `=={cN}==` highlight tokens).
     * Single-sourced from [com.checkit.domain.NestedColorToken] via
     * [com.checkit.domain.HighlightPalette]: one hex per token, append-only,
     * indices are user-visible as `c1`..`c13`.
     */
    val CommonColors: List<String> = com.checkit.domain.HighlightPalette

    /**
     * List icons referenced in icon pickers and task views.
     * Single-sourced from [AppSymbols.listIconKeys] with normalized lowercase keys.
     */
    val ListIcons: List<String> get() = AppSymbols.listIconKeys

    fun icon(name: String): ImageVector = AppSymbols.materialIcon(name)

    fun colorFromHexOrNull(hex: String): Color? {
        val rgb = hex.removePrefix("#").toIntOrNull(16) ?: return null
        return Color(
            red = ((rgb shr 16) and 0xFF) / 255f,
            green = ((rgb shr 8) and 0xFF) / 255f,
            blue = (rgb and 0xFF) / 255f
        )
    }

    fun colorFromHex(hex: String): Color = colorFromHexOrNull(hex) ?: FallbackColor
}

fun materialIcon(name: String): ImageVector = AppIconColorDefaults.icon(name)

fun String.toColor(): Color = AppIconColorDefaults.colorFromHex(this)

fun String.parseHexColorOrNull(): Color? = AppIconColorDefaults.colorFromHexOrNull(this)

fun Color.toHex(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()
}

fun Color.isLight(): Boolean {
    val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
    return luminance > 0.5
}

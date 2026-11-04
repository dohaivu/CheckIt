package com.checkit.domain

import com.checkit.ui.theme.AppIconColorDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Locks the unified 13-color palette: one hex per [NestedColorToken] and the
 * `{cN}` highlight order, both of which are persisted in user data.
 */
class NestedColorPaletteTest {

    @Test
    fun everyConcreteTokenHasAUniqueHex() {
        val hexes = NestedColorToken.entries.map { it.hex() }

        assertEquals(13, hexes.count { it != null })
        assertEquals(13, hexes.filterNotNull().distinct().size)
        assertNull(NestedColorToken.Default.hex())
    }

    @Test
    fun highlightPaletteUsesOneHexPerTokenInLegacyHueOrder() {
        assertEquals(
            listOf(
                "#DC2626",
                "#EA580C",
                "#CA8A04",
                "#65A30D",
                "#16A34A",
                "#0D9488",
                "#0891B2",
                "#2563EB",
                "#6366F1",
                "#7C3AED",
                "#DB2777",
                "#7C2D12",
                "#64748B",
            ),
            HighlightPalette
        )
    }

    @Test
    fun everyPaletteHexBelongsToAToken() {
        val tokenHexes = NestedColorToken.entries.mapNotNull { it.hex() }.toSet()

        assertTrue(HighlightPalette.all { it in tokenHexes })
    }

    @Test
    fun pickerPaletteMatchesHighlightPalette() {
        assertEquals(HighlightPalette, AppIconColorDefaults.CommonColors)
        assertEquals(13, AppIconColorDefaults.CommonColors.size)
    }

    @Test
    fun fallbackColorMatchesGrayToken() {
        assertEquals(NestedColorToken.Gray.hex(), AppIconColorDefaults.FallbackColorHex)
    }
}

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
                "#2563EB", // c1 Blue
                "#6366F1", // c2 Indigo
                "#7C3AED", // c3 Purple
                "#DB2777", // c4 Pink
                "#DC2626", // c5 Red
                "#EA580C", // c6 Orange
                "#CA8A04", // c7 Yellow
                "#65A30D", // c8 Lime
                "#16A34A", // c9 Green
                "#0D9488", // c10 Teal
                "#0891B2", // c11 Cyan
                "#7C2D12", // c12 Brown
                "#64748B"  // c13 Gray
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
    fun highlightTokensResolveFromThePalette() {
        assertEquals(NestedColorToken.Blue.hex(), highlightHexForToken("{c1}"))
        assertEquals(NestedColorToken.Lime.hex(), highlightHexForToken("{c8}"))
        assertEquals(NestedColorToken.Gray.hex(), highlightHexForToken("{c13}"))
        assertNull(highlightHexForToken("{c14}"))
        assertNull(highlightHexForToken("{c0}"))
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

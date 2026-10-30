package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NestedGuideDotTest {

    @Test
    fun blankInputClearsTheEmoji() {
        assertNull(sanitizeGuideDot(null))
        assertNull(sanitizeGuideDot(""))
        assertNull(sanitizeGuideDot("   "))
        assertNull(sanitizeGuideDot("\n\t"))
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertEquals("🚀", sanitizeGuideDot("  🚀 "))
        assertEquals("🚀", sanitizeGuideDot("\n🚀\t"))
    }

    @Test
    fun singleAndMultiCodePointEmojisSurvive() {
        assertEquals("🔥", sanitizeGuideDot("🔥"))
        // Skin tone: two code points.
        assertEquals("🤝🏿", sanitizeGuideDot("🤝🏿"))
        // Zero-width joiner family: well within the cap.
        assertEquals("👨‍👩‍👧‍👦", sanitizeGuideDot("👨‍👩‍👧‍👦"))
    }

    @Test
    fun overlongInputIsRejectedInsteadOfTruncated() {
        // Truncating could split a surrogate pair or a ZWJ sequence, so the
        // value is dropped and the row keeps the default dot.
        val tooLong = "🚀".repeat(NESTED_GUIDE_DOT_MAX_LENGTH / 2 + 1)

        assertNull(sanitizeGuideDot(tooLong))
    }

    @Test
    fun multiCharacterInputAtTheCapIsKept() {
        // Two emojis still fit: the field is decorative, not validated as a
        // single grapheme.
        assertEquals("🚀🚀", sanitizeGuideDot("🚀🚀"))
    }
}

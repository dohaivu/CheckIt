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

    @Test
    fun symbolKeysAreEncodedAndDecoded() {
        val value = GuideDot.fromSymbolKey("Lightbulb")
        assertEquals("sym:lightbulb", value)
        assertEquals(value, sanitizeGuideDot(value))
        assertEquals("lightbulb", GuideDot.symbolKey(value))
        assertNull(GuideDot.emoji(value))
    }

    @Test
    fun emojiValuesAreNotMistakenForSymbols() {
        assertEquals("🔥", GuideDot.emoji("🔥"))
        assertNull(GuideDot.symbolKey("🔥"))
        assertNull(GuideDot.emoji(null))
        assertNull(GuideDot.symbolKey(null))
        assertNull(GuideDot.emoji("sym:"))
    }

    @Test
    fun malformedOrOverlongSymbolKeysAreRejected() {
        assertNull(sanitizeGuideDot("sym:"))
        assertNull(sanitizeGuideDot("sym:Bad Key"))
        assertNull(sanitizeGuideDot("sym:🚀"))
        assertNull(sanitizeGuideDot("sym:" + "a".repeat(NESTED_GUIDE_DOT_MAX_LENGTH + 1)))
        assertEquals("sym:abc_123", sanitizeGuideDot("sym:abc_123"))
    }

    @Test
    fun malformedSymbolKeysDecodeToNullInSymbolKey() {
        assertNull(GuideDot.symbolKey("sym:Bad Key"))
        assertNull(GuideDot.symbolKey("sym:🚀"))
        assertNull(GuideDot.symbolKey("sym:"))
        assertNull(GuideDot.symbolKey("sym:" + "a".repeat(NESTED_GUIDE_DOT_MAX_LENGTH + 1)))
    }

    @Test
    fun fromSymbolKeySafelyHandlesAlreadyPrefixedOrCasedKeys() {
        assertEquals("sym:star", GuideDot.fromSymbolKey("sym:STAR"))
        assertEquals("sym:star", GuideDot.fromSymbolKey("STAR"))
        assertEquals("sym:star", sanitizeGuideDot("sym:STAR"))
        assertEquals("star", GuideDot.symbolKey("sym:STAR"))
    }
}

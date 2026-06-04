package com.checkit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TagParserTest {
    @Test
    fun parsesAsciiVietnameseAndChineseTags() {
        val tags = TagParser.parse("Lunch #food #ăn-trưa 学习 #中文")

        assertEquals(listOf("food", "ăn-trưa", "中文"), tags)
    }

    @Test
    fun deduplicatesTagsAfterNormalization() {
        val tags = TagParser.parse("Coffee #Work #work #WORK")

        assertEquals(listOf("work"), tags)
    }

    @Test
    fun replacesOnlyActiveTagToken() {
        val token = TagParser.activeToken("Lunch #wo", cursorIndex = 9)

        assertEquals("Lunch #work ", TagParser.replaceActiveToken("Lunch #wo", token!!, "work"))
    }

    @Test
    fun ignoresIncompleteTagsAndStopsAtPunctuation() {
        val tags = TagParser.parse("Taxi # #trip, coffee #work.")

        assertEquals(listOf("trip", "work"), tags)
    }

    @Test
    fun activeTokenReturnsNullWhenCursorIsNotAfterHash() {
        assertNull(TagParser.activeToken("#food", cursorIndex = 0))
        assertNull(TagParser.activeToken("Lunch #food", cursorIndex = 6))
    }

    @Test
    fun renameTagInNoteUpdatesMatchingTagsOnly() {
        val note = "Lunch #Food and #foodie, then #FOOD."

        assertEquals(
            "Lunch #meal and #foodie, then #meal.",
            TagParser.renameTagInNote(note, "food", "meal")
        )
    }

    @Test
    fun removeTagFromNoteRemovesMatchingTagsOnly() {
        val note = "Lunch #Food and #foodie, then #FOOD."

        assertEquals(
            "Lunch and #foodie, then.",
            TagParser.removeTagFromNote(note, "food")
        )
    }
}

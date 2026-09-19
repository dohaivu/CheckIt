package com.checkit.ui.journal

import kotlin.test.Test
import kotlin.test.assertEquals

class JournalPromptsTest {

    @Test
    fun countJournalWordsIgnoresMarkdown() {
        assertEquals(0, countJournalWords(""))
        assertEquals(0, countJournalWords("   "))
        assertEquals(2, countJournalWords("hello world"))
        assertEquals(2, countJournalWords("**hello** *world*"))
        assertEquals(3, countJournalWords("# Title\n- one\n- two"))
        assertEquals(3, countJournalWords("1. one\n2. two\n> quote"))
    }

    @Test
    fun readMinutesRoundsUp() {
        assertEquals(0, journalReadMinutes(0))
        assertEquals(1, journalReadMinutes(1))
        assertEquals(1, journalReadMinutes(200))
        assertEquals(2, journalReadMinutes(201))
    }

    @Test
    fun insertSnippetHandlesNewlines() {
        assertEquals("## ", insertJournalSnippet("", "\n## "))
        assertEquals("hi\n- ", insertJournalSnippet("hi", "\n- "))
        assertEquals("hi\n- ", insertJournalSnippet("hi\n", "\n- "))
        assertEquals("hi**text**", insertJournalSnippet("hi", "**text**"))
    }

    @Test
    fun promptsAreSeparateFromLabels() {
        // Labels stay short; prompts carry guidance + templates.
        assertEquals(12, JournalLabels.size)
        assertEquals(6, JournalPrompts.size)
        JournalPrompts.forEach {
            assertEquals(true, it.title.isNotBlank())
            assertEquals(true, it.guidingQuestion.isNotBlank())
        }
    }
}

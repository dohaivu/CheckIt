package com.checkit.ui.journal

import kotlin.test.Test
import kotlin.test.assertEquals

class JournalToolbarTest {

    @Test
    fun boldInsertsPlaceholderAtCollapsedCursor() {
        val edit = applyJournalToolbarAction("hello", 5, 5, JournalToolbarAction.Bold)
        assertEquals("hello**text**", edit.text)
        assertEquals(7, edit.selectionStart)
        assertEquals(11, edit.selectionEnd)
    }

    @Test
    fun boldWrapsSelection() {
        val edit = applyJournalToolbarAction("hello world", 6, 11, JournalToolbarAction.Bold)
        assertEquals("hello **world**", edit.text)
        assertEquals(8, edit.selectionStart)
        assertEquals(13, edit.selectionEnd)
    }

    @Test
    fun bulletPrefixesCurrentLineAtCursor() {
        val edit = applyJournalToolbarAction("line one\nline two", 12, 12, JournalToolbarAction.Bullet)
        assertEquals("line one\n- line two", edit.text)
        assertEquals(14, edit.selectionStart)
    }

    @Test
    fun bulletTogglesOffWhenPresent() {
        val edit = applyJournalToolbarAction("- item", 2, 2, JournalToolbarAction.Bullet)
        assertEquals("item", edit.text)
        assertEquals(0, edit.selectionStart)
    }

    @Test
    fun headingInsertsOnEmpty() {
        val edit = applyJournalToolbarAction("", 0, 0, JournalToolbarAction.Heading)
        assertEquals("## ", edit.text)
        assertEquals(3, edit.selectionStart)
    }

    @Test
    fun quotePrefixesMultilineSelection() {
        val edit = applyJournalToolbarAction("a\nb", 0, 3, JournalToolbarAction.Quote)
        assertEquals("> a\n> b", edit.text)
    }
}

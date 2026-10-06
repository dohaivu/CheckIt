package com.checkit.ui.journal

import com.checkit.ui.components.ToolbarAction
import com.checkit.ui.components.applyToolbarAction
import kotlin.test.Test
import kotlin.test.assertEquals

class JournalToolbarTest {

    @Test
    fun boldInsertsPlaceholderAtCollapsedCursor() {
        val edit = applyToolbarAction("hello", 5, 5, ToolbarAction.Bold)
        assertEquals("hello**text**", edit.text)
        assertEquals(7, edit.selectionStart)
        assertEquals(11, edit.selectionEnd)
    }

    @Test
    fun boldWrapsSelection() {
        val edit = applyToolbarAction("hello world", 6, 11, ToolbarAction.Bold)
        assertEquals("hello **world**", edit.text)
        assertEquals(8, edit.selectionStart)
        assertEquals(13, edit.selectionEnd)
    }

    @Test
    fun italicWrapsSelection() {
        val edit = applyToolbarAction("hello world", 6, 11, ToolbarAction.Italic)
        assertEquals("hello *world*", edit.text)
        assertEquals(7, edit.selectionStart)
        assertEquals(12, edit.selectionEnd)
    }

    @Test
    fun strikethroughWrapsSelection() {
        val edit = applyToolbarAction("hello world", 6, 11, ToolbarAction.Strikethrough)
        assertEquals("hello ~~world~~", edit.text)
        assertEquals(8, edit.selectionStart)
        assertEquals(13, edit.selectionEnd)
    }

    @Test
    fun bulletPrefixesCurrentLineAtCursor() {
        val edit = applyToolbarAction("line one\nline two", 12, 12, ToolbarAction.Bullet)
        assertEquals("line one\n- line two", edit.text)
        assertEquals(14, edit.selectionStart)
    }

    @Test
    fun bulletTogglesOffWhenPresent() {
        val edit = applyToolbarAction("- item", 2, 2, ToolbarAction.Bullet)
        assertEquals("item", edit.text)
        assertEquals(0, edit.selectionStart)
    }

    @Test
    fun headingInsertsOnEmpty() {
        val edit = applyToolbarAction("", 0, 0, ToolbarAction.Heading)
        assertEquals("## ", edit.text)
        assertEquals(3, edit.selectionStart)
    }

    @Test
    fun quotePrefixesMultilineSelection() {
        val edit = applyToolbarAction("a\nb", 0, 3, ToolbarAction.Quote)
        assertEquals("> a\n> b", edit.text)
    }
}

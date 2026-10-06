package com.checkit.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownTextFieldTest {

    @Test
    fun detectSlashQuery_detectsSlashAtStartOfLine() {
        val query = detectSlashQuery("/d", 2)
        assertNotNull(query)
        assertEquals(0, query.slashIndex)
        assertEquals("d", query.query)
    }

    @Test
    fun detectSlashQuery_detectsSlashPrecededByWhitespace() {
        val text = "Hello /time"
        val query = detectSlashQuery(text, text.length)
        assertNotNull(query)
        assertEquals(6, query.slashIndex)
        assertEquals("time", query.query)
    }

    @Test
    fun detectSlashQuery_ignoresSlashInsideWord() {
        val text = "http://example"
        val query = detectSlashQuery(text, text.length)
        assertNull(query)
    }

    @Test
    fun detectSlashQuery_ignoresSlashFollowedBySpace() {
        val text = "/ date"
        val query = detectSlashQuery(text, text.length)
        assertNull(query)
    }

    @Test
    fun applySlashReplacement_replacesSlashQueryWithContent() {
        val text = "Note: /date for review"
        // '/date' starts at index 6, cursor is at index 11 (after 'date')
        val edit = applySlashReplacement(text, 6, 11, "2026-03-30")
        assertEquals("Note: 2026-03-30 for review", edit.text)
        assertEquals(16, edit.selectionStart)
        assertEquals(16, edit.selectionEnd)
    }

    @Test
    fun defaultAppenderCommands_includesDateAndCustomTemplates() {
        val templates = listOf(
            MarkdownTemplate(
                name = "Wins",
                content = "## Wins\n- ",
                description = "Wins template"
            )
        )
        val commands = defaultAppenderCommands(templates)

        val winsCommand = commands.firstOrNull { it.name == "wins" }
        assertNotNull(winsCommand)
        assertEquals("Wins", winsCommand.label)

        val dateCommand = commands.firstOrNull { it.name == "date" }
        assertNotNull(dateCommand)

        val edit = winsCommand.action("Hello /wins", 6, 11)
        assertEquals("Hello ## Wins\n- ", edit.text)
    }

    @Test
    fun currentFormattedDateAndTime_returnsNonEmptyStrings() {
        val dateStr = currentFormattedDate()
        val timeStr = currentFormattedTime()
        val nowStr = currentFormattedNow()

        assertTrue(dateStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(timeStr.matches(Regex("\\d{2}:\\d{2}")))
        assertTrue(nowStr.contains(dateStr) && nowStr.contains(timeStr))
    }
}

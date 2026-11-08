package com.checkit.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppSymbolsTest {

    @Test
    fun testListIconKeysAreAllLowercase() {
        val keys = AppSymbols.listIconKeys
        assertTrue(keys.isNotEmpty(), "List icon keys should not be empty")
        keys.forEach { key ->
            assertEquals(key.lowercase(), key, "Key '$key' must be lowercase")
        }
    }

    @Test
    fun testCalloutSymbolsSfMapping() {
        assertEquals("info.circle", AppSymbols.sfSymbol("info"))
        assertEquals("lightbulb", AppSymbols.sfSymbol("lightbulb"))
        assertEquals("brain.head.profile", AppSymbols.sfSymbol("idea"))
        assertEquals("sparkles", AppSymbols.sfSymbol("sparkles"))
        assertEquals("star", AppSymbols.sfSymbol("summary"))
        assertEquals("exclamationmark.circle", AppSymbols.sfSymbol("important"))
        assertEquals("exclamationmark.triangle", AppSymbols.sfSymbol("warning"))
        assertEquals("exclamationmark.octagon", AppSymbols.sfSymbol("caution"))
        assertEquals("questionmark.circle", AppSymbols.sfSymbol("question"))
        assertEquals("quote.opening", AppSymbols.sfSymbol("quote"))
    }

    @Test
    fun testListIconsSfMapping() {
        assertEquals("tray", AppSymbols.sfSymbol("inbox"))
        assertEquals("list.bullet", AppSymbols.sfSymbol("format_list_bulleted"))
        assertEquals("bookmark", AppSymbols.sfSymbol("bookmark"))
        assertEquals("tag", AppSymbols.sfSymbol("local_offer"))
        assertEquals("house", AppSymbols.sfSymbol("home"))
        assertEquals("briefcase", AppSymbols.sfSymbol("work"))
    }

    @Test
    fun testCaseInsensitiveLookup() {
        assertEquals("tray", AppSymbols.sfSymbol("Inbox"))
        assertEquals("tray", AppSymbols.sfSymbol("INBOX"))
        assertEquals("tray", AppSymbols.sfSymbol("inbox"))

        assertEquals("house", AppSymbols.sfSymbol("Home"))
        assertEquals("house", AppSymbols.sfSymbol("home"))
    }

    @Test
    fun testFallbackForUnknownKey() {
        assertEquals("info.circle", AppSymbols.sfSymbol("unknown_icon_xyz"))
    }

    @Test
    fun testMaterialIconResolutionDoesNotCrash() {
        assertNotNull(AppSymbols.materialIcon("info"))
        assertNotNull(AppSymbols.materialIcon("inbox"))
        assertNotNull(AppSymbols.materialIcon("home"))
        assertNotNull(AppSymbols.materialIcon("unknown_key"))
    }

    @Test
    fun testAppIconColorDefaultsDelegation() {
        assertEquals(AppSymbols.listIconKeys, AppIconColorDefaults.ListIcons)
        assertNotNull(AppIconColorDefaults.icon("inbox"))
    }
}

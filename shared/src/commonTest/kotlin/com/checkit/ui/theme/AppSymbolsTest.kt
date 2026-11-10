package com.checkit.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppSymbolsTest {

    private val keyPattern = Regex("^[a-z][a-z0-9_]*$")

    @Test
    fun testKeysAreLowercaseSnakeCase() {
        AppSymbols.allSymbols.forEach { spec ->
            assertTrue(
                keyPattern.matches(spec.key),
                "Key '${spec.key}' must be lowercase snake_case"
            )
        }
    }

    @Test
    fun testKeysAreUnique() {
        val keys = AppSymbols.allSymbols.map { it.key }
        assertEquals(keys.size, keys.distinct().size, "Symbol keys must be unique")
    }

    @Test
    fun testEverySpecResolvesItsOwnKey() {
        AppSymbols.allSymbols.forEach { spec ->
            assertEquals(spec, AppSymbols.findSpec(spec.key), "Key '${spec.key}' must resolve")
        }
    }

    @Test
    fun testEverySpecHasMaterialVectorMapping() {
        val missing = AppSymbols.allSymbols
            .filter { it.materialIconName !in AppSymbols.mappedMaterialIconNames }
            .map { "${it.key} -> ${it.materialIconName}" }
        assertTrue(missing.isEmpty(), "Missing material icon mappings: $missing")
    }

    @Test
    fun testUsagesAreModelledPerPicker() {
        val info = AppSymbols.findSpec("info")!!
        assertTrue(AppSymbolUsage.Callout in info.usages)
        assertTrue(AppSymbolUsage.ListIcon !in info.usages)

        val inbox = AppSymbols.findSpec("inbox")!!
        assertTrue(AppSymbolUsage.Callout in inbox.usages)
        assertTrue(AppSymbolUsage.ListIcon in inbox.usages)
    }

    @Test
    fun testEverySymbolIsAvailableForCallouts() {
        assertEquals(
            AppSymbols.allSymbols.map { it.key },
            AppSymbols.calloutIconKeys
        )
    }

    @Test
    fun testListIconsAreASubsetOfCalloutSymbols() {
        assertTrue(AppSymbols.listIconKeys.isNotEmpty(), "List icons must not be empty")
        assertTrue(
            AppSymbols.listIconKeys.all { it in AppSymbols.calloutIconKeys },
            "Every list icon should also be a callout symbol"
        )
    }

    @Test
    fun testCalloutSymbolsSfMapping() {
        assertEquals("info.circle", AppSymbols.sfSymbol("info"))
        assertEquals("lightbulb", AppSymbols.sfSymbol("lightbulb"))
        assertEquals("brain.head.profile", AppSymbols.sfSymbol("idea"))
        assertEquals("sparkles", AppSymbols.sfSymbol("sparkles"))
        assertEquals("list.bullet.rectangle", AppSymbols.sfSymbol("summary"))
        assertEquals("exclamationmark.circle", AppSymbols.sfSymbol("important"))
        assertEquals("exclamationmark.triangle", AppSymbols.sfSymbol("warning"))
        assertEquals("exclamationmark.octagon", AppSymbols.sfSymbol("caution"))
        assertEquals("questionmark.circle", AppSymbols.sfSymbol("question"))
        assertEquals("quote.opening", AppSymbols.sfSymbol("quote"))
        assertEquals("chevron.left.forwardslash.chevron.right", AppSymbols.sfSymbol("code"))
    }

    @Test
    fun testExtendedCalloutSymbolsAreRegistered() {
        val calloutKeys = AppSymbols.calloutIconKeys
        assertTrue(calloutKeys.size >= 20, "Callout picker should offer a rich symbol set")
        listOf(
            "success", "shield", "target", "trophy", "key", "paperclip", "image", "file",
            "code", "formula", "calculate", "chart", "trending", "bolt", "thumb_up",
            "thumb_down", "announcement", "science", "place", "map", "language",
            "translate", "bug", "database", "upload", "download", "save", "sync",
            "add", "remove", "close", "stop", "checklist"
        ).forEach { key ->
            assertTrue(key in calloutKeys, "Missing callout symbol '$key'")
        }
    }

    @Test
    fun testListIconsSfMapping() {
        assertEquals("tray", AppSymbols.sfSymbol("inbox"))
        assertEquals("list.bullet", AppSymbols.sfSymbol("list"))
        assertEquals("bookmark", AppSymbols.sfSymbol("bookmark"))
        assertEquals("tag", AppSymbols.sfSymbol("tag"))
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
        assertNull(AppSymbols.findSpec("unknown_icon_xyz"))
    }

    @Test
    fun testMaterialIconResolutionDoesNotCrash() {
        assertNotNull(AppSymbols.materialIcon("info"))
        assertNotNull(AppSymbols.materialIcon("inbox"))
        assertNotNull(AppSymbols.materialIcon("home"))
        assertNotNull(AppSymbols.materialIcon("unknown_key"))
    }

    @Test
    fun testPickersContainNoDuplicateSfSymbols() {
        listOf("callout" to AppSymbols.calloutIconKeys, "list" to AppSymbols.listIconKeys)
            .forEach { (picker, keys) ->
                val symbols = keys.map { AppSymbols.sfSymbol(it) }
                assertEquals(
                    symbols.size,
                    symbols.distinct().size,
                    "The $picker picker must not show duplicate SF symbols"
                )
            }
    }

    @Test
    fun testAppIconColorDefaultsDelegation() {
        assertEquals(AppSymbols.listIconKeys, AppIconColorDefaults.ListIcons)
        assertNotNull(AppIconColorDefaults.icon("inbox"))
    }
}

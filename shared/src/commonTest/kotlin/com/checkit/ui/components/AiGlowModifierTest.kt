package com.checkit.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiGlowModifierTest {

    @Test
    fun geminiSpectrumStopsAreMonotonic() {
        val stops = AiGlowSpectrum.Gemini.stops
        assertTrue(stops.isNotEmpty())
        for (i in 0 until stops.size - 1) {
            assertTrue(stops[i].first <= stops[i + 1].first, "Stops must be in ascending order")
        }
    }

    @Test
    fun ringColorStopsArrayGeneratesSortedStopsForAnyRotation() {
        val stops = AiGlowSpectrum.Gemini.stops
        val testRotations = floatArrayOf(0f, 0.1f, 0.25f, 0.5f, 0.73f, 0.99f, 1f)

        for (rot in testRotations) {
            val result = ringColorStopsArray(rot, stops)
            assertTrue(result.size >= 2, "Result should have at least start and end stops")
            assertEquals(0f, result.first().first, 0.0001f, "First stop should be 0.0")
            assertEquals(1f, result.last().first, 0.0001f, "Last stop should be 1.0")
            assertEquals(result.first().second, result.last().second, "Start and end seam colors must match")

            for (i in 0 until result.size - 1) {
                assertTrue(
                    result[i].first <= result[i + 1].first,
                    "Generated stops for rot=$rot must be strictly monotonically non-decreasing at index $i (${result[i].first} vs ${result[i + 1].first})"
                )
            }
        }
    }

    @Test
    fun interpolateRingColorMatchesExactStops() {
        val stops = AiGlowSpectrum.Gemini.stops
        for (stop in stops) {
            val interpolated = interpolateRingColor(stop.first, stops)
            assertEquals(stop.second, interpolated)
        }
    }

    @Test
    fun interpolateRingColorHandlesClamping() {
        val stops = listOf(0.2f to Color.Red, 0.8f to Color.Blue)
        assertEquals(Color.Red, interpolateRingColor(0.0f, stops))
        assertEquals(Color.Red, interpolateRingColor(0.2f, stops))
        assertEquals(Color.Blue, interpolateRingColor(0.8f, stops))
        assertEquals(Color.Blue, interpolateRingColor(1.0f, stops))
    }
}

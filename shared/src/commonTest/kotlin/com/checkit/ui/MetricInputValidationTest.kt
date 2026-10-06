package com.checkit.ui

import com.checkit.domain.MetricUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetricInputValidationTest {
    @Test
    fun integerUnitsStripNonDigits() {
        assertEquals("123", sanitizeMetricInput(MetricUnit.Points, "1a2b3"))
        assertEquals("123", sanitizeMetricInput(MetricUnit.Items, "12.3"))
        assertEquals("5", sanitizeMetricInput(MetricUnit.Lan, "5 lần"))
        assertEquals("", sanitizeMetricInput(MetricUnit.Points, ""))
        assertEquals("", sanitizeMetricInput(MetricUnit.Points, "abc"))
    }

    @Test
    fun decimalUnitsKeepSingleDotAndLeadingMinus() {
        assertEquals("12.5", sanitizeMetricInput(MetricUnit.Hours, "12.5"))
        assertEquals("12.5", sanitizeMetricInput(MetricUnit.Hours, "12,5"))
        assertEquals("12.57", sanitizeMetricInput(MetricUnit.Hours, "12.5.7"))
        assertEquals("-3", sanitizeMetricInput(MetricUnit.VND, "-3"))
        assertEquals("13", sanitizeMetricInput(MetricUnit.Km, "1-3"))
        assertEquals("7", sanitizeMetricInput(MetricUnit.Days, "7d"))
    }

    @Test
    fun nonNumericUnitsPassThrough() {
        assertEquals("abc 123", sanitizeMetricInput(MetricUnit.None, "abc 123"))
        assertEquals("kg 2x", sanitizeMetricInput(MetricUnit.Custom, "kg 2x"))
        assertEquals("", sanitizeMetricInput(MetricUnit.Countdown, ""))
    }

    @Test
    fun sanitizeNeverClamps() {
        assertEquals("120", sanitizeMetricInput(MetricUnit.Percentage, "120"))
        assertEquals("1000", sanitizeMetricInput(MetricUnit.Points, "1000"))
    }

    @Test
    fun parseAcceptsCommaDecimals() {
        assertEquals(12.5, parseMetricDouble("12,5"))
        assertEquals(12.5, parseMetricDouble(" 12.5 "))
        assertNull(parseMetricDouble(""))
        assertNull(parseMetricDouble("abc"))
    }

    @Test
    fun formatDropsTrailingZero() {
        assertEquals("45", formatMetricDouble(45.0))
        assertEquals("45.5", formatMetricDouble(45.5))
        assertEquals("0", formatMetricDouble(0.0))
        assertEquals("", formatMetricDouble(Double.NaN))
    }

    @Test
    fun unitKindPredicates() {
        assertTrue(MetricUnit.Points.isIntegerUnit())
        assertTrue(MetricUnit.Items.isIntegerUnit())
        assertTrue(MetricUnit.Lan.isIntegerUnit())
        assertFalse(MetricUnit.Hours.isIntegerUnit())
        assertFalse(MetricUnit.Rating.isIntegerUnit())
        assertTrue(MetricUnit.Rating.isNumeric())
        assertTrue(MetricUnit.Percentage.isNumeric())
        assertFalse(MetricUnit.None.isNumeric())
        assertFalse(MetricUnit.Custom.isNumeric())
        assertFalse(MetricUnit.Countdown.isNumeric())
        assertFalse(MetricUnit.DueDate.isNumeric())
    }
}

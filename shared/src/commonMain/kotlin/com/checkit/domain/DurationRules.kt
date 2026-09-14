package com.checkit.domain

/**
 * Single source for shared duration presets (reminder pickers, countdown
 * pickers, ...). Domain-specific objects ([QuickNoteRules], `CountdownManager`)
 * keep their own `const` aliases so platform call sites — including Swift —
 * don't need to change.
 */
object DurationRules {
    const val MINUTE_MILLIS = 60L * 1000L
    const val HOUR_MILLIS = 60L * MINUTE_MILLIS

    const val MIN_5_MILLIS = 5L * MINUTE_MILLIS
    const val MIN_10_MILLIS = 10L * MINUTE_MILLIS
    const val MIN_15_MILLIS = 15L * MINUTE_MILLIS
    const val MIN_30_MILLIS = 30L * MINUTE_MILLIS
    const val HOUR_1_MILLIS = HOUR_MILLIS
}

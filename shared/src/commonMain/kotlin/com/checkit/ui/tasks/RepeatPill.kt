package com.checkit.ui.tasks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.runtime.Composable
import com.checkit.ui.RepeatPreset

@Composable
internal fun RepeatPill(
    repeatRRule: String?
) {
    val label = repeatRRule.repeatLabel() ?: return
    DetailChip(Icons.Default.MoreTime, label)
}

internal fun String?.repeatLabel(): String? {
    val rrule = this ?: return null
    val preset = RepeatPreset.fromRRule(rrule)
    if (preset != RepeatPreset.None) return preset.label

    val frequency = rrule
        .split(";")
        .firstOrNull { it.startsWith("FREQ=") }
        ?.substringAfter("=")

    return when (frequency) {
        "DAILY" -> "Everyday"
        "WEEKLY" -> "Weekly"
        "MONTHLY" -> "Monthly"
        "YEARLY" -> "Yearly"
        else -> "Repeats"
    }
}

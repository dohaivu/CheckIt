package com.checkit.domain

import com.checkit.ui.MinutesPerDay

object NotificationDoNotDisturbPolicy {
    private const val StartMinutes = 22 * 60 // 10 PM
    private const val EndMinutes = 6 * 60 // 6 AM

    fun canNotifyAt(minutes: Int): Boolean {
        val normalized = minutes.mod(MinutesPerDay)
        return normalized in EndMinutes until StartMinutes
    }
}

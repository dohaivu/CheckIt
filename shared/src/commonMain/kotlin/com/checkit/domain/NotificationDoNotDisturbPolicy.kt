package com.checkit.domain

object NotificationDoNotDisturbPolicy {
    private const val StartMinutes = 22 * 60
    private const val EndMinutes = 6 * 60
    private const val MinutesPerDay = 24 * 60

    fun canNotifyAt(minutes: Int): Boolean {
        val normalized = minutes.mod(MinutesPerDay)
        return normalized in EndMinutes until StartMinutes
    }
}

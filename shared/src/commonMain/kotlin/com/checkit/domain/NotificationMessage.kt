package com.checkit.domain

import kotlin.random.Random

data class NotificationMessage(
    val title: String,
    val body: String
) {
    companion object {
        private val CheckInMessages = listOf(
            NotificationMessage(
                title = "A tiny step counts",
                body = "Give your dream five focused minutes. Start small and let momentum find you."
            ),
            NotificationMessage(
                title = "Future you is cheering",
                body = "Add one gentle action to My Day and make the next moment a little easier."
            ),
            NotificationMessage(
                title = "Make it beautifully small",
                body = "Pick one thing you can do in five minutes. Progress loves a tiny doorway."
            ),
            NotificationMessage(
                title = "Your dream gets a vote",
                body = "Choose one small action now. Even a quiet step still moves you forward."
            ),
            NotificationMessage(
                title = "Start where you are",
                body = "No perfect plan needed. Add a five-minute move and begin from this moment."
            ),
            NotificationMessage(
                title = "One spark is enough",
                body = "Capture a note, set a reminder, or do one small task. Let today stay alive."
            ),
            NotificationMessage(
                title = "Give today a little shape",
                body = "My Day is open for one kind next step. What would feel good to finish?"
            ),
            NotificationMessage(
                title = "Small action, real energy",
                body = "Do something tiny for your goal now. Five minutes can change the texture of the day."
            ),
            NotificationMessage(
                title = "Come back to your path",
                body = "Add one clear next move. Your bigger dream is built from moments like this."
            ),
            NotificationMessage(
                title = "A little win is waiting",
                body = "Choose a simple task, set a nudge, or write the thought down before it drifts."
            )
        )

        fun randomCheckIn(random: Random = Random.Default): NotificationMessage =
            CheckInMessages[random.nextInt(CheckInMessages.size)]
    }
}

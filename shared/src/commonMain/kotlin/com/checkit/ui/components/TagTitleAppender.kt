package com.checkit.ui.components

object TagTitleAppender {
    private val tagActionMap = mapOf(
        "cycling" to "đạp xe",
        "coding" to "code ",
        "reading" to "read on",
        "trading" to "trade",
        "focus" to "Focus on ",
        "call" to "Call ",
        "meeting" to "Meeting with ",
        "email" to "Email ",
        "review" to "Review ",
        "buy" to "Buy ",
        "fix" to "Fix ",
        "ideas" to "Explore ",
        "explore" to "Explore ",
        "draft" to "Draft ",
        "write" to "Write ",
        "read" to "Read ",
        "learn" to "Learn ",
        "workout" to "Workout: ",
        "exercise" to "Exercise: ",
        "meditate" to "Meditate on ",
    )

    fun appendTagActionText(currentTitle: String, tagName: String): String {
        val actionText = tagActionMap[tagName.lowercase().trim()] ?: return currentTitle
        val separator = if (currentTitle.isEmpty() || currentTitle.endsWith(" ")) "" else " "
        return "$currentTitle$separator$actionText"
    }
}

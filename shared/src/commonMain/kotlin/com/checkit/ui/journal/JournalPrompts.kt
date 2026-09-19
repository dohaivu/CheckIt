package com.checkit.ui.journal

/** Long-form guided prompt. Separate from the free-text label. */
data class JournalPrompt(
    val id: String,
    val title: String,
    val guidingQuestion: String,
    val template: String
)

internal val JournalPrompts = listOf(
    JournalPrompt(
        id = "free_write",
        title = "Free write",
        guidingQuestion = "Write without editing for 5 minutes. What is top of mind?",
        template = ""
    ),
    JournalPrompt(
        id = "gratitude",
        title = "Gratitude",
        guidingQuestion = "What are you thankful for? Be specific — who, what, why?",
        template = "I am grateful for:\n1. \n2. \n3. \n\nWhy this matters:\n"
    ),
    JournalPrompt(
        id = "morning_intention",
        title = "Morning intention",
        guidingQuestion = "What would make today feel meaningful?",
        template = "Today I want to feel:\n\nOne thing that matters:\n\nI will let go of:\n"
    ),
    JournalPrompt(
        id = "evening_review",
        title = "Evening review",
        guidingQuestion = "How did today go? Wins, friction, lessons?",
        template = "- **Win**:\n- **Friction**:\n- **Insight**:\n\nTomorrow I will:\n"
    ),
    JournalPrompt(
        id = "hard_thing",
        title = "Hard thing",
        guidingQuestion = "What is weighing on you? Name it, then what is next?",
        template = "What happened:\n\nWhat I feel:\n\nWhat I need / next step:\n"
    ),
    JournalPrompt(
        id = "idea",
        title = "Idea",
        guidingQuestion = "Capture the idea before it fades. What, for whom, next?",
        template = "Idea:\n\nProblem:\n\nNext small step:\n"
    )
)

internal fun findJournalPrompt(id: String?): JournalPrompt? =
    id?.let { needle -> JournalPrompts.firstOrNull { it.id == needle } }

/** Word count that ignores common markdown markers. */
internal fun countJournalWords(content: String): Int {
    if (content.isBlank()) return 0
    val stripped = content
        .replace(Regex("\\*\\*|\\*|`|~~"), " ")
        .replace(Regex("(?m)^\\s*#{1,6}\\s"), "")
        .replace(Regex("(?m)^\\s*[-*]\\s"), "")
        .replace(Regex("(?m)^\\s*\\d+\\.\\s"), "")
        .replace(Regex("^>\\s?", RegexOption.MULTILINE), "")
    return stripped.split(Regex("\\s+")).count { it.isNotBlank() }
}

internal fun journalReadMinutes(wordCount: Int): Int =
    if (wordCount <= 0) 0 else ((wordCount + 199) / 200).coerceAtLeast(1)

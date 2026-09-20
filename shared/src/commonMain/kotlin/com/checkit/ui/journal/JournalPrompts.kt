package com.checkit.ui.journal

enum class JournalPromptCategory {
    StateOfMind,
    Reflection,
    Capture
}

/** Long-form guided prompt. Separate from the free-text label. */
data class JournalPrompt(
    val id: String,
    val title: String,
    val guidingQuestion: String,
    val template: String,
    val category: JournalPromptCategory = JournalPromptCategory.Reflection,
    val suitablePeriods: Set<JournalPeriod> = setOf(
        JournalPeriod.Morning,
        JournalPeriod.Afternoon,
        JournalPeriod.Evening
    )
)

internal val JournalPrompts = listOf(
    JournalPrompt(
        id = "free_write",
        title = "Free write",
        guidingQuestion = "Write without editing for 5 minutes. What is top of mind?",
        template = "",
        category = JournalPromptCategory.Capture,
        suitablePeriods = setOf(JournalPeriod.Morning, JournalPeriod.Afternoon, JournalPeriod.Evening)
    ),
    JournalPrompt(
        id = "weather_report",
        title = "Weather report",
        guidingQuestion = "If your mind were weather, what is it? Stormy, foggy, clear spells?",
        template = "Right now:\n\nIn my body:\n\nOn my mind:\n",
        category = JournalPromptCategory.StateOfMind,
        suitablePeriods = setOf(JournalPeriod.Morning, JournalPeriod.Afternoon)
    ),
    JournalPrompt(
        id = "name_it",
        title = "Name it",
        guidingQuestion = "What exactly do you feel? How strong? Where in your body?",
        template = "Feeling:\n\nIntensity /10:\n\nWhere I feel it:\n\nWhat triggered it:\n",
        category = JournalPromptCategory.StateOfMind,
        suitablePeriods = setOf(JournalPeriod.Morning, JournalPeriod.Afternoon, JournalPeriod.Evening)
    ),
    JournalPrompt(
        id = "gratitude",
        title = "Gratitude",
        guidingQuestion = "What are you thankful for? Be specific — who, what, why?",
        template = "I am grateful for:\n1. \n2. \n3. \n\nWhy this matters:\n",
        category = JournalPromptCategory.StateOfMind,
        suitablePeriods = setOf(JournalPeriod.Morning, JournalPeriod.Evening)
    ),
    JournalPrompt(
        id = "morning_intention",
        title = "Morning intention",
        guidingQuestion = "What would make today feel meaningful?",
        template = "Today I want to feel:\n\nOne thing that matters:\n\nI will let go of:\n",
        category = JournalPromptCategory.Reflection,
        suitablePeriods = setOf(JournalPeriod.Morning)
    ),
    JournalPrompt(
        id = "evening_review",
        title = "Evening review",
        guidingQuestion = "How did today go? Wins, friction, lessons?",
        template = "- **Win**:\n- **Friction**:\n- **Insight**:\n\nTomorrow I will:\n",
        category = JournalPromptCategory.Reflection,
        suitablePeriods = setOf(JournalPeriod.Evening)
    ),
    JournalPrompt(
        id = "what_i_need",
        title = "What I need",
        guidingQuestion = "Not what you should do — what would feel caring right now?",
        template = "What hurts:\n\nWhat I need:\n\nOne small kindness:\n",
        category = JournalPromptCategory.StateOfMind,
        suitablePeriods = setOf(JournalPeriod.Afternoon, JournalPeriod.Evening)
    ),
    JournalPrompt(
        id = "unsent_letter",
        title = "Unsent letter",
        guidingQuestion = "Write to the person or situation. You won't send it.",
        template = "Dear…:\n\nI feel…:\n\nI wish…:\n",
        category = JournalPromptCategory.StateOfMind,
        suitablePeriods = setOf(JournalPeriod.Evening)
    ),
    JournalPrompt(
        id = "hard_thing",
        title = "Hard thing",
        guidingQuestion = "What is weighing on you? Name it, then what is next?",
        template = "What happened:\n\nWhat I feel:\n\nWhat I need / next step:\n",
        category = JournalPromptCategory.StateOfMind,
        suitablePeriods = setOf(JournalPeriod.Afternoon, JournalPeriod.Evening)
    ),
    JournalPrompt(
        id = "golden_moment",
        title = "Golden moment",
        guidingQuestion = "What small good moment deserves a second look?",
        template = "Moment:\n\nWhy it mattered:\n\nHow to keep it:\n",
        category = JournalPromptCategory.StateOfMind,
        suitablePeriods = setOf(JournalPeriod.Morning, JournalPeriod.Afternoon)
    ),
    JournalPrompt(
        id = "kind_voice",
        title = "Kind voice",
        guidingQuestion = "What would you say to a friend in your shoes?",
        template = "What I tell myself:\n\nWhat I'd tell a friend:\n\nKinder version:\n",
        category = JournalPromptCategory.StateOfMind,
        suitablePeriods = setOf(JournalPeriod.Morning, JournalPeriod.Evening)
    ),
    JournalPrompt(
        id = "worry_underneath",
        title = "Underneath the worry",
        guidingQuestion = "What's the worst story? Most likely? What steadies you?",
        template = "Worry:\n\nWorst story:\n\nMost likely:\n\nOne steadying step:\n",
        category = JournalPromptCategory.StateOfMind,
        suitablePeriods = setOf(JournalPeriod.Afternoon, JournalPeriod.Evening)
    ),
    JournalPrompt(
        id = "idea",
        title = "Idea",
        guidingQuestion = "Capture the idea before it fades. What, for whom, next?",
        template = "Idea:\n\nProblem:\n\nNext small step:\n",
        category = JournalPromptCategory.Capture,
        suitablePeriods = setOf(JournalPeriod.Afternoon)
    )
)

internal fun findJournalPrompt(id: String?): JournalPrompt? =
    id?.let { needle -> JournalPrompts.firstOrNull { it.id == needle } }

/** Time-aware ordering: suitable-for-period first, otherwise stable order. */
internal fun orderedJournalPrompts(nowMinutes: Int): List<JournalPrompt> {
    val period = nowMinutes.toJournalPeriod()
    val (suitable, rest) = JournalPrompts.partition { period in it.suitablePeriods }
    return suitable + rest
}

/** Mood-to-prompt relevance, ordered by priority. Emoji inputs, bucket logic inside. */
internal fun moodRelevantPromptIds(moods: Collection<String>): List<String> {
    if (moods.isEmpty()) return emptyList()
    val emojiToBucket = MoodCategories.flatMap { (bucket, emojis) -> emojis.map { it to bucket } }.toMap()
    val buckets = moods.mapNotNull { emojiToBucket[it] }.distinct().toSet()
    if (buckets.isEmpty()) return emptyList()
    val ordered = mutableListOf<String>()
    fun add(vararg ids: String) {
        ids.forEach { if (it !in ordered) ordered.add(it) }
    }
    if ("Worried" in buckets) add("worry_underneath", "name_it", "kind_voice")
    if ("Sad" in buckets) add("kind_voice", "unsent_letter", "name_it")
    if ("Tired" in buckets) add("what_i_need", "kind_voice", "free_write")
    if ("Happy" in buckets) add("gratitude", "golden_moment")
    if ("Loved" in buckets) add("gratitude", "golden_moment", "unsent_letter")
    if ("Calm" in buckets) add("free_write", "golden_moment", "gratitude")
    if ("Energetic" in buckets) add("morning_intention", "idea", "free_write")
    if ("Focused" in buckets) add("morning_intention", "idea", "evening_review")
    return ordered.filter { id -> JournalPrompts.any { it.id == id } }
}

/** Period ordering with mood-relevant prompts boosted to the front. */
internal fun orderedJournalPrompts(nowMinutes: Int, moods: Collection<String>): List<JournalPrompt> {
    val base = orderedJournalPrompts(nowMinutes)
    if (moods.isEmpty()) return base
    val relevant = moodRelevantPromptIds(moods)
    if (relevant.isEmpty()) return base
    val byId = base.associateBy { it.id }
    val moodOrdered = relevant.mapNotNull { byId[it] }
    val moodIds = moodOrdered.map { it.id }.toSet()
    return moodOrdered + base.filter { it.id !in moodIds }
}

internal fun suggestedPromptIds(
    nowMinutes: Int,
    moods: Collection<String> = emptyList(),
    limit: Int = 2
): Set<String> = orderedJournalPrompts(nowMinutes, moods).take(limit).map { it.id }.toSet()

internal fun suggestedPrompt(nowMinutes: Int, hasEntryToday: Boolean): JournalPrompt? =
    orderedJournalPrompts(nowMinutes).firstOrNull()

/** Feeling vocabulary per mood bucket. Static, editor-only; saved data stays emoji-only. */
internal val FeelingWords: Map<String, List<String>> = mapOf(
    "Happy" to listOf("grateful", "proud", "playful", "hopeful", "content", "excited"),
    "Energetic" to listOf("motivated", "restless", "wired", "driven", "jittery", "alive"),
    "Calm" to listOf("at ease", "grounded", "spacious", "tender", "settled", "soft"),
    "Loved" to listOf("seen", "held", "warm", "connected", "tender", "safe"),
    "Focused" to listOf("clear", "in flow", "steady", "determined", "sharp", "absorbed"),
    "Tired" to listOf("drained", "foggy", "heavy", "sleepy", "flat", "weary"),
    "Worried" to listOf("anxious", "overwhelmed", "uneasy", "restless", "insecure", "dread"),
    "Sad" to listOf("lonely", "disappointed", "empty", "homesick", "blue", "grieving")
)

/** Feeling words for the currently selected mood emojis. Saved data stays emoji-only. */
internal fun feelingWordsForMoods(moods: Collection<String>): List<String> {
    if (moods.isEmpty()) return emptyList()
    val emojiToBucket = MoodCategories.flatMap { (bucket, emojis) -> emojis.map { it to bucket } }.toMap()
    return moods.mapNotNull { emojiToBucket[it] }
        .distinct()
        .flatMap { FeelingWords[it].orEmpty() }
        .distinct()
}

/** Gentle follow-up nudge for stuck writing. Null when nothing needed. */
internal fun followUpForWords(wordCount: Int): String? = when {
    wordCount <= 0 -> null
    wordCount < 30 -> "Where do you feel that in your body?"
    wordCount < 80 -> "What triggered this today?"
    wordCount < 150 -> "What do you need right now?"
    else -> null
}

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

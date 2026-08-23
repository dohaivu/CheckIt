package com.checkit.domain

val MoodHappyEmojis = listOf("😀", "😃", "😄", "😊", "🥳", "✨", "💛", "🌈", "🎈", "☀️")
val MoodEnergeticEmojis = listOf("🔥", "⚡", "🤩", "🚀", "🎉", "💪", "🧡", "🎸", "🏆", "🏃")
val MoodCalmEmojis = listOf("😌", "🌿", "🌊", "🧘", "🕊️", "☁️", "💚", "🍃", "🛶", "🕯️")
val MoodSadEmojis = listOf("😢", "😭", "😔", "🌧️", "💔", "☁️", "💙", "🥀", "🌑", "📻")
val MoodFocusedEmojis = listOf("🎯", "💻", "📚", "🧠", "✍️", "🧐", "💜", "🛠️", "♟️", "🧪")
val MoodTiredEmojis = listOf("😴", "🥱", "🔋", "💤", "🛌", "🚶", "🤎", "☕", "🔌", "🏠")
val MoodWorriedEmojis = listOf("😟", "😰", "😨", "😦", "😧", "😖", "😬", "🫨", "🆘", "⚠️")
val MoodLovedEmojis = listOf("🥰", "😍", "😘", "💖", "🌹", "🧸", "❤️", "🥂", "💍", "💌")

/** Coarse mood categories offered as journal-history filters. */
enum class MoodFilter(val label: String, val emojis: List<String>) {
    Good(
        "Good",
        MoodHappyEmojis + MoodEnergeticEmojis + MoodCalmEmojis + MoodLovedEmojis
    ),
    Bad(
        "Bad",
        MoodSadEmojis + MoodTiredEmojis + MoodWorriedEmojis
    )
}

fun JournalEntry.isGoodMood(): Boolean {
    val goodMoods = MoodHappyEmojis + MoodEnergeticEmojis + MoodCalmEmojis + MoodLovedEmojis
    return moods.any { it in goodMoods }
}

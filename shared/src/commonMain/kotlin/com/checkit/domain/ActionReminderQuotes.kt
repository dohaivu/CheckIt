package com.checkit.domain

import kotlin.random.Random

data class ActionReminderQuote(
    val text: String,
    val author: String
) {
    val line: String = "$text - $author"
}

object ActionReminderQuotes {
    val Quotes: List<ActionReminderQuote> = listOf(
        ActionReminderQuote("Well begun is half done.", "Aristotle"),
        ActionReminderQuote("Do the thing, and you shall have the power.", "Ralph Waldo Emerson"),
        ActionReminderQuote("The secret of getting ahead is getting started.", "Mark Twain"),
        ActionReminderQuote("Waste no more time arguing what a good person should be. Be one.", "Marcus Aurelius"),
        ActionReminderQuote("Act as if what you do makes a difference. It does.", "William James"),
        ActionReminderQuote("Action is the foundational key to all success.", "Pablo Picasso"),
        ActionReminderQuote("Make each day your masterpiece.", "John Wooden"),
        ActionReminderQuote("Begin, be bold, and venture to be wise.", "Horace"),
        ActionReminderQuote("He who moves not forward, goes backward.", "Johann Wolfgang von Goethe"),
        ActionReminderQuote("The way to get started is to quit talking and begin doing.", "Walt Disney"),
        ActionReminderQuote("Great acts are made up of small deeds.", "Lao Tzu"),
        ActionReminderQuote("Do not wait to strike till the iron is hot; make it hot by striking.", "William Butler Yeats"),
        ActionReminderQuote("What you do today can improve all your tomorrows.", "Ralph Marston"),
        ActionReminderQuote("The future depends on what you do today.", "Mahatma Gandhi"),
        ActionReminderQuote("Either you run the day or the day runs you.", "Jim Rohn"),
        ActionReminderQuote("Do what you can, with what you have, where you are.", "Theodore Roosevelt"),
        ActionReminderQuote("Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
        ActionReminderQuote("The reward of a thing well done is having done it.", "Ralph Waldo Emerson"),
        ActionReminderQuote("It always seems impossible until it is done.", "Nelson Mandela"),
        ActionReminderQuote("Never confuse motion with action.", "Benjamin Franklin"),
        ActionReminderQuote("Lost time is never found again.", "Benjamin Franklin"),
        ActionReminderQuote("To think too long about doing a thing often becomes its undoing.", "Eva Young"),
        ActionReminderQuote("The beginning is the most important part of the work.", "Plato"),
        ActionReminderQuote("Small deeds done are better than great deeds planned.", "Peter Marshall"),
        ActionReminderQuote("Energy and persistence conquer all things.", "Benjamin Franklin"),
        ActionReminderQuote("The best way out is always through.", "Robert Frost"),
        ActionReminderQuote("Do not wait; the time will never be just right.", "Napoleon Hill"),
        ActionReminderQuote("Nothing will work unless you do.", "Maya Angelou"),
        ActionReminderQuote("You cannot plow a field by turning it over in your mind.", "Gordon B. Hinckley"),
        ActionReminderQuote("The great thing is to begin.", "Charles Spurgeon"),
        ActionReminderQuote("To improve is to change.", "Winston Churchill"),
        ActionReminderQuote("Let deeds match words.", "Plautus"),
        ActionReminderQuote("Deeds, not words.", "Latin proverb"),
        ActionReminderQuote("The work praises the worker.", "Irish proverb"),
        ActionReminderQuote("Little strokes fell great oaks.", "Benjamin Franklin"),
        ActionReminderQuote("Who begins has half the deed done.", "Horace")
    )

    fun random(random: Random = Random.Default): ActionReminderQuote =
        Quotes[random.nextInt(Quotes.size)]
}

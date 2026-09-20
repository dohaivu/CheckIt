package com.checkit.ui.journal

import kotlin.test.Test
import kotlin.test.assertEquals

class JournalPromptsTest {

    @Test
    fun countJournalWordsIgnoresMarkdown() {
        assertEquals(0, countJournalWords(""))
        assertEquals(0, countJournalWords("   "))
        assertEquals(2, countJournalWords("hello world"))
        assertEquals(2, countJournalWords("**hello** *world*"))
        assertEquals(3, countJournalWords("# Title\n- one\n- two"))
        assertEquals(3, countJournalWords("1. one\n2. two\n> quote"))
    }

    @Test
    fun readMinutesRoundsUp() {
        assertEquals(0, journalReadMinutes(0))
        assertEquals(1, journalReadMinutes(1))
        assertEquals(1, journalReadMinutes(200))
        assertEquals(2, journalReadMinutes(201))
    }

    @Test
    fun insertSnippetHandlesNewlines() {
        assertEquals("## ", insertJournalSnippet("", "\n## "))
        assertEquals("hi\n- ", insertJournalSnippet("hi", "\n- "))
        assertEquals("hi\n- ", insertJournalSnippet("hi\n", "\n- "))
        assertEquals("hi**text**", insertJournalSnippet("hi", "**text**"))
    }

    @Test
    fun promptsAreSeparateFromLabels() {
        // Labels stay short; prompts carry guidance + templates.
        assertEquals(12, JournalLabels.size)
        assertEquals(13, JournalPrompts.size)
        JournalPrompts.forEach {
            assertEquals(true, it.title.isNotBlank())
            assertEquals(true, it.guidingQuestion.isNotBlank())
        }
    }

    @Test
    fun orderingPrefersSuitablePeriod() {
        val morning = orderedJournalPrompts(8 * 60).map { it.id }
        assertEquals(true, morning.indexOf("morning_intention") < morning.indexOf("evening_review"))
        val evening = orderedJournalPrompts(21 * 60).map { it.id }
        assertEquals(true, evening.indexOf("evening_review") < evening.indexOf("morning_intention"))
    }

    @Test
    fun moodBoostsRelevantPromptsToFront() {
        val worriedEmoji = MoodCategories.first { it.first == "Worried" }.second.first()
        val ordered = orderedJournalPrompts(8 * 60, listOf(worriedEmoji)).map { it.id }
        assertEquals("worry_underneath", ordered.first())
        assertEquals(true, suggestedPromptIds(8 * 60, listOf(worriedEmoji)).contains("worry_underneath"))

        val happyEmoji = MoodCategories.first { it.first == "Happy" }.second.first()
        val happyOrdered = orderedJournalPrompts(21 * 60, listOf(happyEmoji)).map { it.id }
        assertEquals(true, happyOrdered.indexOf("gratitude") < happyOrdered.indexOf("evening_review"))

        assertEquals(true, moodRelevantPromptIds(emptyList()).isEmpty())
    }

    @Test
    fun followUpNudgesEscalate() {
        assertEquals(null, followUpForWords(0))
        assertEquals("Where do you feel that in your body?", followUpForWords(10))
        assertEquals("What triggered this today?", followUpForWords(50))
        assertEquals("What do you need right now?", followUpForWords(100))
        assertEquals(null, followUpForWords(200))
    }

    @Test
    fun feelingWordsResolveFromMoods() {
        val worriedEmoji = MoodCategories.first { it.first == "Worried" }.second.first()
        val words = feelingWordsForMoods(listOf(worriedEmoji))
        assertEquals(true, "anxious" in words)
        assertEquals(true, words.isNotEmpty())
        assertEquals(0, feelingWordsForMoods(emptyList()).size)
    }

    @Test
    fun suggestionCtaUsesPeriodTone() {
        val morning = suggestedPrompt(8 * 60, false)!!
        assertEquals(true, suggestionCtaLabel(8 * 60, false, morning).contains(morning.title))
        val evening = suggestedPrompt(21 * 60, true)!!
        assertEquals(true, suggestionCtaLabel(21 * 60, true, evening).contains(evening.title))
    }

    @Test
    fun greetingsAreVariedAndShort() {
        JournalPeriod.entries.forEach { period ->
            val greetings = greetingsForPeriod(period)
            assertEquals(true, greetings.size >= 3)
            assertEquals(greetings.size, greetings.distinct().size)
            greetings.forEach { assertEquals(true, it.length <= 60) }
        }
    }
}

package com.checkit.ui.components

import androidx.compose.ui.graphics.Color
import com.checkit.domain.*

// Define your app's mood palette
val MoodHappy = Color(0xFFFFD54F)      // Warm Yellow
val MoodEnergetic = Color(0xFFFF8A65)  // Vibrant Orange
val MoodCalm = Color(0xFF81C784)       // Soft Green
val MoodSad = Color(0xFF64B5F6)        // Soft Blue
val MoodFocused = Color(0xFF9575CD)    // Deep Purple
val MoodTired = Color(0xFFA1887F)      // Muted Brown
val MoodWorried = Color(0xFFEF5350)   // Urgent Red
val MoodLoved = Color(0xFFF06292)      // Soft Pink
val MoodDefault = Color(0xFF9E9E9E)    // Neutral Gray

// Helper function to extract color from emoji
fun getMoodColorFromEmoji(emoji: String): Color {
    return when (emoji) {
        in MoodHappyEmojis -> MoodHappy
        in MoodEnergeticEmojis -> MoodEnergetic
        in MoodCalmEmojis -> MoodCalm
        in MoodSadEmojis -> MoodSad
        in MoodFocusedEmojis -> MoodFocused
        in MoodTiredEmojis -> MoodTired
        in MoodWorriedEmojis -> MoodWorried
        in MoodLovedEmojis -> MoodLoved
        else -> MoodDefault
    }
}

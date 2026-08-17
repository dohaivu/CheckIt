package com.checkit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString

@Composable
fun String?.asAnnotatedString(): AnnotatedString {
    val text = this ?: ""
    return remember(text) {
        parseMarkdownToAnnotatedString(text)
    }
}

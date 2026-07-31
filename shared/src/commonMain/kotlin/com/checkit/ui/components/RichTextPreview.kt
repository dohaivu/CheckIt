package com.checkit.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun RichTextPreview(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val state = rememberRichTextState()
    state.setMarkdown(markdown)
    state.config.listIndent = 15

    RichText(
        state = state,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = maxLines,
        minLines = minLines
    )
}

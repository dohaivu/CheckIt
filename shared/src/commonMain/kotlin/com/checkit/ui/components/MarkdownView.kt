package com.checkit.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Markdown(
        modifier = modifier,
        content = markdown,
        typography = markdownTypography(
            h1 = MaterialTheme.typography.headlineSmall,
            h2 = MaterialTheme.typography.titleLarge,
            h3 = MaterialTheme.typography.titleMedium,
            h4 = MaterialTheme.typography.bodyMedium,
            h5 = MaterialTheme.typography.bodySmall,
            h6 = MaterialTheme.typography.bodySmall,
            text = style,
            paragraph = style,
            ordered = style,
            bullet = style,
            list = style
        ),
        colors = markdownColor(text = color),
    )
}

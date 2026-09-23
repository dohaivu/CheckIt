package com.checkit.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.LazyMarkdownSuccess
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.model.rememberMarkdownState

@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val markdownState = rememberMarkdownState(markdown)

    Markdown(
        markdownState = markdownState,
        modifier = modifier,
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
        padding = markdownPadding(listItemTop = 0.dp, listItemBottom = 0.dp),
        colors = markdownColor(text = color),
        success = { state, components, modifier ->
            LazyMarkdownSuccess(state, components, modifier, contentPadding = PaddingValues(16.dp))
        },
    )
}

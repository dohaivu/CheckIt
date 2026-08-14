package com.checkit.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import checkit.shared.generated.resources.Res
import checkit.shared.generated.resources.clear_text
import com.checkit.ui.tasks.views.ContentContainerAlpha
import androidx.compose.ui.unit.sp

@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    ),
    placeholder: String? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    clearEnabled: Boolean = false,
    readOnly: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
) {
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = MaterialTheme.colorScheme.surfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        interactionSource = interactionSource,
        textStyle = textStyle,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = maxLines == 1,
                placeholder = if (placeholder != null) {
                        {
                            Text(
                                text = placeholder,
                                fontStyle = textStyle.fontStyle,
                                fontWeight = textStyle.fontWeight,
                                fontSize = textStyle.fontSize,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentContainerAlpha),
                            )
                        }
                    } else null,
                trailingIcon = trailingIcon ?: if (clearEnabled) {
                    {
                        if (value.isNotEmpty()) {
                            IconButton(
                                onClick = { onValueChange("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(Res.string.clear_text),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else null,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding = contentPadding,
                colors = colors,
            )
        }
    )
}

class MarkdownVisualTransformation : VisualTransformation {
    private val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    private val italicRegex = Regex("\\*(.*?)\\*")
    // Matches any digit followed by a period and a space (e.g., "1. ", "12. ")
    private val numberedListRegex = Regex("^\\d+\\.\\s")

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text

        val transformed = buildAnnotatedString {
            // 1. Put down the raw text first
            append(rawText)

            // 2. Format Line-Based Elements: Headers & Lists
            var currentLineStart = 0
            val lines = rawText.split('\n')

            lines.forEach { line ->
                val lineLength = line.length

                when {
                    // Header 1: "# "
                    line.startsWith("# ") -> {
                        addStyle(
                            style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            start = currentLineStart,
                            end = currentLineStart + lineLength
                        )
                    }
                    // Header 2: "## "
                    line.startsWith("## ") -> {
                        addStyle(
                            style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            start = currentLineStart,
                            end = currentLineStart + lineLength
                        )
                    }
                    // Header 3: "### "
                    line.startsWith("### ") -> {
                        addStyle(
                            style = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                            start = currentLineStart,
                            end = currentLineStart + lineLength
                        )
                    }
                    // Bullet Lists: Starts with "- " or "* " (followed by space, not bolding)
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        // Style just the marker symbol to make it look clean
                        addStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            start = currentLineStart,
                            end = currentLineStart + 2
                        )
                    }
                    // Numbered Lists: Starts with digits like "1. " or "2. "
                    numberedListRegex.find(line) != null -> {
                        val match = numberedListRegex.find(line)!!
                        val markerLength = match.value.length
                        // Style the number prefix to keep spacing consistent
                        addStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            ),
                            start = currentLineStart,
                            end = currentLineStart + markerLength
                        )
                    }
                }
                // Move index tracker past the current line and its \n newline token
                currentLineStart += lineLength + 1
            }

            // 3. Format Inline Elements: Bold (**text**)
            boldRegex.findAll(rawText).forEach { matchResult ->
                val range = matchResult.range
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = range.first,
                    end = range.last + 1
                )
            }

            // 4. Format Inline Elements: Italic (*text*)
            italicRegex.findAll(rawText).forEach { matchResult ->
                val range = matchResult.range
                addStyle(
                    style = SpanStyle(fontStyle = FontStyle.Italic),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }

        return TransformedText(transformed, OffsetMapping.Identity)
    }
}

fun parseMarkdownToAnnotatedString(markdown: String?): AnnotatedString {
    if (markdown.isNullOrEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        val lines = markdown.split('\n')

        lines.forEachIndexed { index, line ->
            var cleanLine = line
            var isHeader = false
            var headerStyle: SpanStyle? = null

            // 1. Process Line-Based Elements (Headers & Lists)
            when {
                cleanLine.startsWith("# ") -> {
                    cleanLine = cleanLine.removePrefix("# ")
                    isHeader = true
                    headerStyle = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                cleanLine.startsWith("## ") -> {
                    cleanLine = cleanLine.removePrefix("## ")
                    isHeader = true
                    headerStyle = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                cleanLine.startsWith("### ") -> {
                    cleanLine = cleanLine.removePrefix("### ")
                    isHeader = true
                    headerStyle = SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                cleanLine.startsWith("- ") || cleanLine.startsWith("* ") -> {
                    cleanLine = "•  " + cleanLine.substring(2)
                }
            }

            // Track exactly where this line starts in our main builder string
            val lineStartIndex = this.length

            // 2. Clear markdown inline symbols and calculate exact styling positions
            val (finalLineText, stylesToApply) = processInlineStyles(cleanLine)

            // Append only the clean text without structural markers
            append(finalLineText)
            val lineEndIndex = this.length

            // Apply header styling if matched
            if (isHeader && headerStyle != null) {
                addStyle(headerStyle, lineStartIndex, lineEndIndex)
            }

            // Apply all saved bold and italic styles using their adjusted positions
            stylesToApply.forEach { styleMarker ->
                addStyle(
                    style = styleMarker.style,
                    start = lineStartIndex + styleMarker.start,
                    end = lineStartIndex + styleMarker.end
                )
            }

            // Add a newline character for all lines except the last one
            if (index < lines.lastIndex) {
                append("\n")
            }
        }
    }
}

// A simple helper data class to store layout positions
private data class StyleMarker(val style: SpanStyle, val start: Int, val end: Int)

private fun processInlineStyles(inputLine: String): Pair<String, List<StyleMarker>> {
    val styles = mutableListOf<StyleMarker>()
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    val italicRegex = Regex("\\*(.*?)\\*")

    // Step A: Parse Bold first
    var workingText = inputLine
    var boldMatch = boldRegex.find(workingText)

    while (boldMatch != null) {
        val fullMatchText = boldMatch.groupValues[0] // e.g. "**bold text**"
        val innerText = boldMatch.groupValues[1]     // e.g. "bold text"
        val matchIndex = boldMatch.range.first

        // Save where the clean inner text will sit
        styles.add(
            StyleMarker(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = matchIndex,
                end = matchIndex + innerText.length
            )
        )

        // Remove the symbols by replacing the full match with the inner text
        workingText = workingText.replaceFirst(fullMatchText, innerText)
        boldMatch = boldRegex.find(workingText)
    }

    // Step B: Parse Italic second on the cleaned string
    var italicMatch = italicRegex.find(workingText)
    while (italicMatch != null) {
        val fullMatchText = italicMatch.groupValues[0] // e.g. "*italic text*"
        val innerText = italicMatch.groupValues[1]     // e.g. "italic text"
        val matchIndex = italicMatch.range.first

        styles.add(
            StyleMarker(
                style = SpanStyle(fontStyle = FontStyle.Italic),
                start = matchIndex,
                end = matchIndex + innerText.length
            )
        )

        workingText = workingText.replaceFirst(fullMatchText, innerText)
        italicMatch = italicRegex.find(workingText)
    }

    return Pair(workingText, styles)
}

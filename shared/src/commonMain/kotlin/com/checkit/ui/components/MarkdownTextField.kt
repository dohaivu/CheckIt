package com.checkit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.checkit.ui.journal.JournalToolbarAction
import com.checkit.ui.journal.applyJournalToolbarAction
import com.checkit.ui.tasks.views.ContentContainerAlpha

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkdownToolbar(
    onAction: (JournalToolbarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        listOf(
            "B" to JournalToolbarAction.Bold,
            "I" to JournalToolbarAction.Italic,
            "~~" to JournalToolbarAction.Strikethrough,
            "H" to JournalToolbarAction.Heading,
            "• List" to JournalToolbarAction.Bullet,
            "1. List" to JournalToolbarAction.Numbered,
            "Quote" to JournalToolbarAction.Quote
        ).forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onAction(action) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun MarkdownTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface
    ),
    placeholder: String? = null,
    placeholderStyle: TextStyle = textStyle.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentContainerAlpha)
    ),
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    showToolbar: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
    textFieldModifier: Modifier = Modifier.fillMaxWidth()
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }
    var lastExternalText by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    if (value != lastExternalText) {
        lastExternalText = value
        textFieldValue = TextFieldValue(
            text = value,
            selection = TextRange(value.length)
        )
    }

    Column(modifier = modifier) {
        AppOutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                if (newValue.text != lastExternalText) {
                    lastExternalText = newValue.text
                    onValueChange(newValue.text)
                }
            },
            textStyle = textStyle,
            placeholder = placeholder,
            placeholderStyle = placeholderStyle,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            enabled = enabled,
            readOnly = readOnly,
            interactionSource = interactionSource,
            visualTransformation = remember { MarkdownVisualTransformation() },
            contentPadding = contentPadding,
            modifier = textFieldModifier
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
        )

        if (showToolbar && isFocused && enabled && !readOnly) {
            Spacer(Modifier.height(6.dp))
            MarkdownToolbar(
                onAction = { action ->
                    val edit = applyJournalToolbarAction(
                        text = textFieldValue.text,
                        selectionStart = textFieldValue.selection.start,
                        selectionEnd = textFieldValue.selection.end,
                        action = action
                    )
                    val newTextFieldValue = textFieldValue.copy(
                        text = edit.text,
                        selection = TextRange(edit.selectionStart, edit.selectionEnd)
                    )
                    textFieldValue = newTextFieldValue
                    lastExternalText = edit.text
                    onValueChange(edit.text)
                    focusRequester.requestFocus()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

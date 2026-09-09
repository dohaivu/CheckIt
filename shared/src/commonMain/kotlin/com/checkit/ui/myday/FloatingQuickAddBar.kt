package com.checkit.ui.myday

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.checkit.ui.components.AiQuickAddBar

/**
 * Slim floating bottom bar for capturing a plan item right now.
 * Shown only when nothing on today's plan sits within ±30 minutes of the current time.
 */
@Composable
internal fun FloatingQuickAddBar(
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AiQuickAddBar(
        value = text,
        onValueChange = { text = it },
        placeholder = "What's on your mind?",
        modifier = modifier,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                val title = text.trim()
                if (title.isNotEmpty()) onSubmit(title)
                text = ""
                focusManager.clearFocus()
            }
        )
    )
}
